from dataclasses import dataclass


@dataclass
class SizedCustomField:

    def __init__(self, value: int):
        self.value = value

    def parse(span: bytes) -> 'SizedCustomField':
        return SizedCustomField(span[0])

    @property
    def size(self) -> int:
        return 1


@dataclass
class UnsizedCustomField:

    def __init__(self, value: int):
        self.value = value

    def parse(span: bytes) -> 'UnsizedCustomField':
        return UnsizedCustomField(span[0])

    @property
    def size(self) -> int:
        return 1


def Checksum(span: bytes) -> int:
    return 0
