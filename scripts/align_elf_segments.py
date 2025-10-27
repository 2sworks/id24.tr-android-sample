#!/usr/bin/env python3
"""
Align ELF LOAD segments to 16KB (0x4000) page size for Android 15+ compatibility.
This script modifies the ELF file in-place to ensure all LOAD segments are 16KB aligned.
"""

import sys
import struct
import os

def align_to_16kb(value):
    """Align value to 16KB boundary (0x4000)"""
    alignment = 0x4000
    return (value + alignment - 1) & ~(alignment - 1)

def align_elf_to_16kb(filepath):
    """Align all LOAD segments in ELF file to 16KB page size"""

    if not os.path.exists(filepath):
        print(f"Error: File not found: {filepath}")
        return False

    with open(filepath, 'rb') as f:
        data = bytearray(f.read())

    # Check ELF magic
    if data[0:4] != b'\x7fELF':
        print(f"Error: Not an ELF file: {filepath}")
        return False

    # Determine if 32-bit or 64-bit
    ei_class = data[4]
    is_64bit = (ei_class == 2)

    # Determine endianness
    ei_data = data[5]
    is_little_endian = (ei_data == 1)
    endian = '<' if is_little_endian else '>'

    if is_64bit:
        # ELF64
        # e_phoff (program header offset) at offset 0x20
        # e_phnum (number of program headers) at offset 0x38
        phoff = struct.unpack(endian + 'Q', data[0x20:0x28])[0]
        phnum = struct.unpack(endian + 'H', data[0x38:0x3a])[0]
        phentsize = 56  # Size of program header in ELF64

        print(f"ELF64: {phnum} program headers at offset 0x{phoff:x}")

        modified = False
        for i in range(phnum):
            ph_offset = phoff + i * phentsize

            # Read program header
            p_type = struct.unpack(endian + 'I', data[ph_offset:ph_offset+4])[0]

            # PT_LOAD = 1
            if p_type == 1:
                # p_align is at offset 0x30 in the program header
                align_offset = ph_offset + 0x30
                current_align = struct.unpack(endian + 'Q', data[align_offset:align_offset+8])[0]

                # If not already 16KB aligned, update it
                if current_align < 0x4000:
                    print(f"  LOAD segment {i}: changing alignment from 0x{current_align:x} to 0x4000")
                    struct.pack_into(endian + 'Q', data, align_offset, 0x4000)
                    modified = True
                else:
                    print(f"  LOAD segment {i}: already aligned to 0x{current_align:x}")
    else:
        # ELF32
        phoff = struct.unpack(endian + 'I', data[0x1c:0x20])[0]
        phnum = struct.unpack(endian + 'H', data[0x2c:0x2e])[0]
        phentsize = 32  # Size of program header in ELF32

        print(f"ELF32: {phnum} program headers at offset 0x{phoff:x}")

        modified = False
        for i in range(phnum):
            ph_offset = phoff + i * phentsize

            p_type = struct.unpack(endian + 'I', data[ph_offset:ph_offset+4])[0]

            if p_type == 1:  # PT_LOAD
                align_offset = ph_offset + 0x1c
                current_align = struct.unpack(endian + 'I', data[align_offset:align_offset+4])[0]

                if current_align < 0x4000:
                    print(f"  LOAD segment {i}: changing alignment from 0x{current_align:x} to 0x4000")
                    struct.pack_into(endian + 'I', data, align_offset, 0x4000)
                    modified = True
                else:
                    print(f"  LOAD segment {i}: already aligned to 0x{current_align:x}")

    if modified:
        # Write back the modified ELF
        with open(filepath, 'wb') as f:
            f.write(data)
        print(f"✓ Successfully aligned {filepath} to 16KB")
        return True
    else:
        print(f"✓ {filepath} already 16KB aligned")
        return True

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print("Usage: python3 align_elf_segments.py <path_to_elf_file>")
        sys.exit(1)

    filepath = sys.argv[1]
    success = align_elf_to_16kb(filepath)
    sys.exit(0 if success else 1)
