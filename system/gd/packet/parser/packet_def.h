/*
 * Copyright 2019 The Android Open Source Project
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

#pragma once

#include <map>
#include <variant>

#include "enum_def.h"
#include "field_list.h"
#include "fields/packet_field.h"
#include "parent_def.h"

class RustParseAndMatchFields {
 private:
  std::map<std::string, std::vector<std::string>> parse_and_match_fields;
  std::set<std::string> CollectInitialParseAndMatchFields(
      const ParentDef* parent, std::map<std::string, std::set<std::string>>& initial_parse_and_match_fields) const;
  void FinalizeParseAndMatchFields(
      const ParentDef* parent,
      std::map<std::string, std::set<std::string>>& initial_parse_and_match_fields,
      std::vector<std::string>& available_fields);

 public:
  RustParseAndMatchFields(const ParentDef* root);
  std::vector<std::string>& GetParseMethodParams(const std::string& packet_name);
  std::vector<std::string>& GetMatchVariables(const std::string& packet_name);
};

class PacketDef : public ParentDef {
 public:
  PacketDef(std::string name, FieldList fields);
  PacketDef(std::string name, FieldList fields, PacketDef* parent);

  PacketField* GetNewField(const std::string& name, ParseLocation loc) const;

  void GenParserDefinition(std::ostream& s) const;

  void GenTestingParserFromBytes(std::ostream& s) const;

  void GenParserDefinitionPybind11(std::ostream& s) const;

  void GenParserFieldGetter(std::ostream& s, const PacketField* field) const;

  void GenValidator(std::ostream& s) const;

  void GenParserToString(std::ostream& s) const;

  TypeDef::Type GetDefinitionType() const;

  void GenBuilderDefinition(std::ostream& s) const;

  void GenBuilderDefinitionPybind11(std::ostream& s) const;

  void GenTestDefine(std::ostream& s) const;

  void GenFuzzTestDefine(std::ostream& s) const;

  FieldList GetParametersToValidate() const;

  void GenBuilderCreate(std::ostream& s) const;

  void GenBuilderCreatePybind11(std::ostream& s) const;

  void GenBuilderParameterChecker(std::ostream& s) const;

  void GenBuilderConstructor(std::ostream& s) const;

  void GenTestingFromView(std::ostream& s) const;

  void GenRustChildEnums(std::ostream& s) const;

  void GenRustStructDeclarations(std::ostream& s) const;

  bool GenRustStructFieldNameAndType(std::ostream& s) const;

  void GenRustStructFieldNames(std::ostream& s) const;

  void GenRustStructImpls(std::ostream& s) const;

  void GenRustAccessStructImpls(std::ostream& s) const;

  void GenRustBuilderStructImpls(std::ostream& s) const;

  void GenRustBuilderTest(std::ostream& s) const;

  void GenRustDef(std::ostream& s) const;
};
