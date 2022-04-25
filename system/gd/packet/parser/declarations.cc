/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "declarations.h"

#include "fields/all_fields.h"

using ConstraintMap = std::map<std::string, std::variant<int64_t, std::string>>;

std::string transform_identifier(std::string ident) {
  if (ident == "_payload_")
    return "payload";
  else if (ident == "_body_")
    return "body";
  else
    return ident;
}

bool parse_constraints(Json::Value constraints, ConstraintMap& map) {
  for (auto constraint : constraints) {
    auto id = constraint["id"].asString();
    auto loc = ParseLocation(constraint["loc"]["start"]["line"].asInt());

    if (constraint["tag_id"]) {
      auto tag_id = constraint["tag_id"].asString();
      map.insert(std::pair(id, std::variant<int64_t, std::string>(tag_id)));
    } else if (constraint["value"]) {
      auto integer = constraint["value"].asInt64();
      map.insert(std::pair(id, std::variant<int64_t, std::string>(integer)));
    } else {
      ERRORLOC(loc) << "Unable to get value from constraint";
      return false;
    }
  }
  return true;
}

bool parse_fields(Json::Value fields, Declarations& declarations, FieldList& list) {
  for (auto field : fields) {
    auto kind = field["kind"].asString();
    auto loc = ParseLocation(field["loc"]["start"]["line"].asInt());

    if (kind == "checksum_field") {
      auto field_id = field["field_id"].asString();

      list.AppendField(new ChecksumStartField(field_id, loc));
    } else if (kind == "padding_field") {
      auto width = field["width"].asInt();

      list.AppendField(new PaddingField(width, loc));
    } else if (kind == "size_field") {
      auto field_id = transform_identifier(field["field_id"].asString());
      auto width = field["width"].asInt();

      list.AppendField(new SizeField(field_id, width, loc));
    } else if (kind == "count_field") {
      auto field_id = transform_identifier(field["field_id"].asString());
      auto width = field["width"].asInt();

      list.AppendField(new CountField(field_id, width, loc));
    } else if (kind == "body_field") {
      list.AppendField(new BodyField(loc));
    } else if (kind == "payload_field") {
      auto size_modifier = field["size_modifier"].asString();

      list.AppendField(new PayloadField(size_modifier, loc));
    } else if (kind == "fixed_field") {
      if (field["width"]) {
        auto width = field["width"].asInt();
        auto value = field["value"].asInt();

        list.AppendField(new FixedScalarField(width, value, loc));
      } else {
        auto enum_id = field["enum_id"].asString();
        auto tag_id = field["tag_id"].asString();

        if (auto type_def = declarations.GetTypeDef(enum_id)) {
          EnumDef* enum_def = (type_def->GetDefinitionType() == TypeDef::Type::ENUM ? (EnumDef*)type_def : nullptr);

          list.AppendField(new FixedEnumField(enum_def, tag_id, loc));
        } else {
          ERRORLOC(loc) << "No enum found with name " << enum_id;
        }
      }
    } else if (kind == "reserved_field") {
      auto width = field["width"].asInt();

      list.AppendField(new ReservedField(width, loc));
    } else if (kind == "array_field") {
      auto id = field["id"].asString();
      auto width = field["width"];
      auto size = field["size"];
      auto size_modifier = field["size_modifier"].asString();

      if (width.isNumeric()) {
        if (size.isNumeric()) {
          list.AppendField(new ArrayField(id, width.asInt(), size.asInt(), loc));
        } else {
          list.AppendField(new VectorField(id, width.asInt(), size_modifier, loc));
        }
      } else {
        auto type_id = field["type_id"].asString();

        if (auto type_def = declarations.GetTypeDef(type_id)) {
          if (size.isNumeric()) {
            list.AppendField(new ArrayField(id, type_def, size.asInt(), loc));
          } else {
            list.AppendField(new VectorField(id, type_def, size_modifier, loc));
          }
        } else {
          ERRORLOC(loc) << "Can't find type used in array field.";
        }
      }
    } else if (kind == "scalar_field") {
      auto id = field["id"].asString();
      auto width = field["width"].asInt();

      list.AppendField(new ScalarField(id, width, loc));
    } else if (kind == "typedef_field") {
      auto id = field["id"].asString();
      auto type_id = field["type_id"].asString();

      if (auto type_def = declarations.GetTypeDef(type_id)) {
        list.AppendField(type_def->GetNewField(id, loc));
      } else {
        ERRORLOC(loc) << "No type with this name " << type_id;
        return false;
      }
    } else if (kind == "group_field") {
      auto group_id = field["group_id"].asString();

      auto constraints = ConstraintMap();

      if (!parse_constraints(field["constraints"], constraints)) return false;

      if (auto group = declarations.GetGroupDef(group_id)) {
        for (const auto field : *group) {
          const auto constraint = constraints.find(field->GetName());
          if (constraint != constraints.end()) {
            if (field->GetFieldType() == ScalarField::kFieldType) {
              DEBUG() << "Fixing group scalar value\n";
              list.AppendField(
                  new FixedScalarField(field->GetSize().bits(), std::get<int64_t>(constraint->second), loc));
            } else if (field->GetFieldType() == EnumField::kFieldType) {
              DEBUG() << "Fixing group enum value\n";
              auto type_def = declarations.GetTypeDef(field->GetDataType());
              EnumDef* enum_def = (type_def->GetDefinitionType() == TypeDef::Type::ENUM ? (EnumDef*)type_def : nullptr);
              if (enum_def == nullptr) {
                ERRORLOC(loc) << "No enum found of type " << field->GetDataType();
                return false;
              }
              if (!enum_def->HasEntry(std::get<std::string>(constraint->second))) {
                ERRORLOC(loc) << "Enum " << field->GetDataType() << " has no enumeration "
                              << std::get<std::string>(constraint->second);
                return false;
              }

              list.AppendField(new FixedEnumField(enum_def, std::get<std::string>(constraint->second), loc));
            } else {
              ERRORLOC(loc) << "Unimplemented constraint of type " << field->GetFieldType();
              return false;
            }
            constraints.erase(constraint);
          } else {
            list.AppendField(field);
          }
        }
      } else {
        ERRORLOC(loc) << "Could not find group with name " << group_id;
        return false;
      }
    } else {
      ERRORLOC(loc) << "unexpected field kind " << kind;
      return false;
    }
  }
  return true;
}

bool Declarations::FromJson(Json::Value json) {
  auto endianness = json["endianness"]["value"].asString();
  auto endianness_loc = ParseLocation(json["endianness"]["loc"]["start"]["line"].asInt());

  if (endianness != "little_endian" && endianness != "big_endian") {
    ERRORLOC(endianness_loc) << "unexpected endianness value " << endianness;
    return false;
  }

  is_little_endian = endianness == "little_endian";

  for (auto declaration : json["declarations"]) {
    auto kind = declaration["kind"].asString();
    auto loc = ParseLocation(declaration["loc"]["start"]["line"].asInt());

    if (kind == "packet_declaration") {
      auto id = declaration["id"].asString();
      auto fields = FieldList();
      auto constraints = ConstraintMap();
      PacketDef* parent = nullptr;

      if (!parse_fields(declaration["fields"], *this, fields)) return false;
      if (!parse_constraints(declaration["constraints"], constraints)) return false;

      if (declaration["parent_id"].isString()) {
        auto parent_id = declaration["parent_id"].asString();
        parent = GetPacketDef(parent_id);
        if (parent == nullptr) {
          ERRORLOC(loc) << "Could not find packet " << parent_id << " used as parent for " << id;
          return false;
        }
      }

      auto def = new PacketDef(id, fields, parent);

      if (parent != nullptr) {
        parent->children_.push_back(def);
      }

      def->AssignSizeFields();

      for (const auto& constraint : constraints) {
        const auto& constraint_name = constraint.first;
        const auto& constraint_value = constraint.second;
        def->AddParentConstraint(constraint_name, constraint_value);
      }

      def->SetEndianness(is_little_endian);

      AddPacketDef(id, def);
    } else if (kind == "struct_declaration") {
      auto id = declaration["id"].asString();
      auto fields = FieldList();
      auto constraints = ConstraintMap();
      TypeDef* parent = nullptr;

      if (!parse_fields(declaration["fields"], *this, fields)) return false;
      if (!parse_constraints(declaration["constraints"], constraints)) return false;

      if (declaration["parent_id"].isString()) {
        auto parent_id = declaration["parent_id"].asString();
        parent = GetTypeDef(parent_id);
        if (parent == nullptr) {
          ERRORLOC(loc) << "Could not find struct " << parent_id << " used as parent for " << id;
          return false;
        }
        if (parent->GetDefinitionType() != TypeDef::Type::STRUCT) {
          ERRORLOC(loc) << parent_id << " is not a struct";
          return false;
        }
      }

      auto def = new StructDef(id, fields, (StructDef*)parent);
      def->AssignSizeFields();

      for (const auto& constraint : constraints) {
        const auto& constraint_name = constraint.first;
        const auto& constraint_value = constraint.second;
        def->AddParentConstraint(constraint_name, constraint_value);
      }

      def->SetEndianness(is_little_endian);

      AddTypeDef(id, def);
    } else if (kind == "enum_declaration") {
      auto id = declaration["id"].asString();
      auto width = declaration["width"].asInt();

      auto def = new EnumDef(id, width);

      for (auto tag : declaration["tags"]) {
        def->AddEntry(tag["id"].asString(), tag["value"].asUInt64());
      }

      AddTypeDef(id, def);
    } else if (kind == "group_declaration") {
      auto id = declaration["id"].asString();
      auto fields = new FieldList();

      if (!parse_fields(declaration["fields"], *this, *fields)) return false;

      AddGroupDef(id, fields);
    } else if (kind == "custom_field_declaration") {
      auto id = declaration["id"].asString();
      auto width = declaration["width"];
      auto function = declaration["function"].asString();

      if (width) {
        AddTypeDef(id, new CustomFieldDef(id, function, width.asInt()));
      } else {
        AddTypeDef(id, new CustomFieldDef(id, function));
      }
    } else if (kind == "checksum_declaration") {
      auto id = declaration["id"].asString();
      auto width = declaration["width"].asInt();
      auto function = declaration["function"].asString();

      AddTypeDef(id, new ChecksumDef(id, function, width));
    } else {
      ERRORLOC(loc) << "unexpected declaration kind " << kind;
      return false;
    }
  }

  return true;
}
