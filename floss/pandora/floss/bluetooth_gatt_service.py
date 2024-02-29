from uuid import UUID


class Service:

    def __init__(self, instance_id=None, service_type=None, uuid=None, characteristics=None, included_services=None):
        self.instance_id = instance_id
        self.service_type = service_type
        self.uuid = uuid
        self.characteristics = characteristics
        self.included_services = included_services

    def to_dict(self):
        return {
            "instance_id": self.instance_id,
            "service_type": self.service_type,
            "uuid": bytearray.fromhex(self.uuid),
            "included_services": [service.to_dict() for service in self.included_services],
            "characteristics": [characteristic.to_dict() for characteristic in self.characteristics],
        }


class Characteristic:

    def __init__(self,
                 instance_id=None,
                 permissions=None,
                 write_type=None,
                 descriptors=None,
                 uuid=None,
                 key_size=None,
                 properties=None):
        self.instance_id = instance_id
        self.permissions = permissions
        self.write_type = write_type
        self.descriptors = descriptors
        self.uuid = uuid
        self.key_size = key_size
        self.properties = properties

    def to_dict(self):
        return {
            "properties": self.properties,
            "permissions": self.permissions,
            "uuid": bytearray.fromhex(self.uuid),
            "instance_id": self.instance_id,
            "descriptors": [descriptor.to_dict() for descriptor in self.descriptors],
            "key_size": self.key_size,
            "write_type": self.write_type,
        }


class Descriptor:

    def __init__(self, permissions=None, uuid=None, instance_id=None):
        self.permissions = permissions
        self.uuid = uuid
        self.instance_id = instance_id

    def to_dict(self):
        return {"instance_id": self.instance_id, "permissions": self.permissions, "uuid": bytearray.fromhex(self.uuid)}


def create_gatt_service(service):
    return Service(
        instance_id=service['instance_id'],
        service_type=service['service_type'],
        uuid=str(UUID(bytes=bytes(service['uuid']))).upper(),
        included_services=[create_gatt_service(included_service) for included_service in service['included_services']],
        characteristics=[create_gatt_characteristic(characteristic) for characteristic in service['characteristics']])


def create_gatt_characteristic(characteristic):
    return Characteristic(
        properties=characteristic['properties'],
        permissions=characteristic['permissions'],
        uuid=str(UUID(bytes=bytes(characteristic['uuid']))).upper(),
        instance_id=characteristic['instance_id'],
        descriptors=[create_gatt_characteristic_descriptor(descriptor) for descriptor in characteristic['descriptors']],
        key_size=characteristic['key_size'],
        write_type=characteristic['write_type'])


def create_gatt_characteristic_descriptor(descriptor):
    return Descriptor(instance_id=descriptor['instance_id'],
                      permissions=descriptor['permissions'],
                      uuid=str(UUID(bytes=bytes(descriptor['uuid']))).upper())


def convert_object_to_dict(obj):
    if isinstance(obj, (Descriptor, Characteristic, Service)):
        return obj.to_dict()
    elif isinstance(obj, list):
        return [convert_object_to_dict(item) for item in obj]
    else:
        return obj
