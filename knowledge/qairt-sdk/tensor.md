# QairtTensor

> Extracted from the Qualcomm QAIRT SDK manual (`#QAIRT/` Drive capture). Official Qualcomm documentation — text extracted from the saved doc page; nav/boilerplate stripped.

Updated: Jul 02, 2026 80-63442-10 Rev: AL

Note: Some methods in this module are not yet implemented in the current release and will raise an exception if called. See the C API for full functionality.

Include: `#include "QairtCppApi/QairtTensor.hpp"`

C++ wrapper for the QAIRT tensor API. Provides data-type, quantization-encoding, and tensor classes used to describe and transfer tensor data across QAIRT backends. `namespace qairt`

## Enums

### `enum class DataType : std::underlying_type_t<Qairt_DataType_t>`

Element data types supported for tensor data.

| Enumerator | Description |
|---|---|
| `Int2` | Signed 2-bit integer. |
| `Int4` | Signed 4-bit integer. |
| `Int8` | Signed 8-bit integer. |
| `Int16` | Signed 16-bit integer. |
| `Int32` | Signed 32-bit integer. |
| `Int64` | Signed 64-bit integer. |
| `UInt2` | Unsigned 2-bit integer. |
| `UInt4` | Unsigned 4-bit integer. |
| `UInt8` | Unsigned 8-bit integer. |
| `UInt16` | Unsigned 16-bit integer. |
| `UInt32` | Unsigned 32-bit integer. |
| `UInt64` | Unsigned 64-bit integer. |
| `Float4` | 4-bit floating-point. |
| `Float8` | 8-bit floating-point. |
| `Float16` | 16-bit floating-point (IEEE 754 half precision). |
| `BFloat16` | 16-bit brain floating-point (bfloat16). |
| `Float32` | 32-bit floating-point (IEEE 754 single). |
| `Float64` | 64-bit floating-point (IEEE 754 double). |
| `SFixedPoint2` | Signed 2-bit fixed-point. |
| `SFixedPoint4` | Signed 4-bit fixed-point. |
| `SFixedPoint8` | Signed 8-bit fixed-point. |
| `SFixedPoint16` | Signed 16-bit fixed-point. |
| `SFixedPoint32` | Signed 32-bit fixed-point. |
| `UFixedPoint2` | Unsigned 2-bit fixed-point. |
| `UFixedPoint4` | Unsigned 4-bit fixed-point. |
| `UFixedPoint8` | Unsigned 8-bit fixed-point. |
| `UFixedPoint16` | Unsigned 16-bit fixed-point. |
| `UFixedPoint32` | Unsigned 32-bit fixed-point. |
| `Bool8` | 8-bit boolean. |
| `String` | Variable-length string. |
| `Undefined` | Unspecified or unknown data type. |

Values map 1:1 to `QAIRT_DATATYPE_*` C constants, e.g. `Int2` = `QAIRT_DATATYPE_INT_2`, `UInt64` = `QAIRT_DATATYPE_UINT_64`, `Float32` = `QAIRT_DATATYPE_FLOAT_32`, `SFixedPoint32` = `QAIRT_DATATYPE_SFIXED_POINT_32`, `UFixedPoint32` = `QAIRT_DATATYPE_UFIXED_POINT_32`, `Bool8` = `QAIRT_DATATYPE_BOOL_8`, `String` = `QAIRT_DATATYPE_STRING`, `Undefined` = `QAIRT_DATATYPE_UNDEFINED` (full naming pattern applies uniformly across all bit-widths and signedness variants above).

### `enum class QuantizationEncoding : std::underlying_type_t<Qairt_QuantizationEncoding_t>`

Quantization encoding types for tensor data.

| Enumerator | Description |
|---|---|
| `ScaleOffset` | Per-tensor scale-offset encoding. |
| `AxisScaleOffset` | Per-axis (e.g., per-channel) scale-offset encoding. |
| `BwScaleOffset` | Bit-width scale-offset encoding. |
| `BwAxisScaleOffset` | Bit-width per-axis scale-offset encoding. |
| `Block` | Per-block scale-offset encoding. |
| `BlockwiseExpansion` | Blockwise expansion encoding. |
| `Vector` | Vector quantization (VQ) compression encoding. |
| `BwAxisScaleOffsetMapped` | Bit-width per-axis scale-offset encoding with mapping. |
| `BwBlockMapped` | Bit-width per-block scale-offset encoding with mapping. |
| `BwBlockwiseExpansionMapped` | Bit-width blockwise expansion encoding with mapping. |
| `FloatBlock` | Per-block float scale-offset encoding. |
| `BwFloatBlock` | Bit-width per-block float scale-offset encoding. |
| `Microscaling` | Microscaling (MX) encoding. |
| `Undefined` | Unused sentinel; present to ensure a 32-bit enum storage. |

Values map 1:1 to `QAIRT_QUANTIZATION_ENCODING_*` C constants (e.g. `ScaleOffset` = `QAIRT_QUANTIZATION_ENCODING_SCALE_OFFSET`, `Undefined` = `QAIRT_QUANTIZATION_ENCODING_UNDEFINED`).

### `enum class QuantizationEncodingMapping : std::underlying_type_t<Qairt_QuantizationEncodingMapping_t>`

Quantized value mapping schemes for scale-offset encodings.

| Enumerator | Description |
|---|---|
| `StandardSymmetric` | Standard symmetric two's complement mapping. |
| `AsymmetricPlusOne` | Two's complement mapping with a positive shift of one. |
| `LinearSymmetricExcludeZero` | Linear mapping symmetric about zero, excluding zero from the range. |
| `Undefined` | Unused sentinel; present to ensure a 32-bit enum storage. |

Values map to `QAIRT_QUANTIZATION_ENCODING_MAPPING_STANDARD_SYMMETRIC` / `_ASYMMETRIC_PLUS_ONE` / `_LINEAR_SYMMETRIC_EXCLUDE_ZERO` / `_UNDEFINED`.

### `enum class FloatEncoding : std::underlying_type_t<Qairt_FloatEncoding_t>`

Floating-point sub-format encodings used in Microscaling (MX) quantization.

| Enumerator | Description |
|---|---|
| `MXFP8_E5M2` | MXFP8 format with 5 exponent bits and 2 mantissa bits; compatible with Float8. |
| `MXFP8_E4M3` | MXFP8 format with 4 exponent bits and 3 mantissa bits; compatible with Float8. |
| `MXFP6_E3M2` | MXFP6 format with 3 exponent bits and 2 mantissa bits. |
| `MXFP6_E2M3` | MXFP6 format with 2 exponent bits and 3 mantissa bits. |
| `MXFP4_E2M1` | MXFP4 format with 2 exponent bits and 1 mantissa bit. |
| `Undefined` | Unused sentinel; present to ensure a 32-bit enum storage. |

Values map to `QAIRT_FLOAT_ENCODING_MXFP8_E5M2` / `_MXFP8_E4M3` / `_MXFP6_E3M2` / `_MXFP6_E2M3` / `_MXFP4_E2M1` / `_UNDEFINED`.

### `enum class TensorMemType : std::underlying_type_t<QairtTensor_MemoryType_t>`

Memory access strategies for tensor data.

| Enumerator | Description |
|---|---|
| `Raw` | Raw memory pointer provided directly by the client. |
| `MemHandle` | Shared memory object handle enabling memory sharing across backends. |
| `RetrieveRaw` | Callback-based retrieval; the backend calls client-supplied callbacks to fetch raw data. |
| `Undefined` | Unused sentinel; present to ensure a 32-bit enum storage. |

Values map to `QAIRT_TENSORMEMTYPE_RAW` / `_MEMHANDLE` / `_RETRIEVE_RAW` / `_UNDEFINED`.

### `enum class BlockwiseExpansionBlockScaleStorageType : std::underlying_type_t<Qairt_BlockwiseExpansionBlockScaleStorageType_t>`

Storage bit-widths for block scales in blockwise expansion quantization.

| Enumerator | Description |
|---|---|
| `Storage8` | Block scales stored as 8-bit values. |
| `Storage16` | Block scales stored as 16-bit values. |
| `Undefined` | Unused sentinel; present to ensure a 32-bit enum storage. |

Values map to `QAIRT_BLOCKWISE_EXPANSION_BITWIDTH_SCALE_STORAGE_8` / `_16` / `_UNDEFINED`.

## `class AxisScaleOffset : public qairt::ApiType<AxisScaleOffset, QairtQuantizeParams_AxisScaleOffsetV1_t>`

`#include <QairtTensor.hpp>`

Per-axis (e.g., per-channel) collection of scale-offset pairs for axis scale-offset quantization. Construct directly: `AxisScaleOffset()`. Set the quantization axis via `setAxis()` and supply one `ScaleOffset` per axis element via `setScaleOffsets()`. Attach to a `QuantizeParams` via `QuantizeParams::setAxisScaleOffsetEncoding()`.

### Public Functions
```cpp
inline ~AxisScaleOffset()
AxisScaleOffset() noexcept = default
inline AxisScaleOffset(AxisScaleOffset &&other) noexcept
inline AxisScaleOffset &operator=(AxisScaleOffset &&other) noexcept
inline AxisScaleOffset(const AxisScaleOffset &other)
inline AxisScaleOffset &operator=(const AxisScaleOffset &other)
inline AxisScaleOffset shallowCopy() const
```
- `inline int32_t getAxis() const` — see also `QairtQuantizeParams_AxisScaleOffset_getAxis`.
- `inline void setAxis(int32_t axis) const` — see also `QairtQuantizeParams_AxisScaleOffset_setAxis`.
- `inline std::vector<ScaleOffset> &getScaleOffsets()` — get the per-axis scale-offset pairs for this encoding. Throws `qairt::Exception` on invalid handle. Returns reference to the vector of `ScaleOffset` objects, one per axis element.
- `inline const std::vector<ScaleOffset> &getScaleOffsets() const` — const-access wrapper; see also `AxisScaleOffset::getScaleOffsets()`.
- `inline void setScaleOffsets(const std::vector<ScaleOffset> &scaleOffsets)` — set the per-axis scale-offset pairs for this encoding. Parameters: `scaleOffsets` [in] scale-offset pairs, one per element along the quantization axis. Throws `qairt::Exception` on invalid handle.

Also inherits the standard `ApiType` constructor/assignment set (template cross-type constructor, table-copy constructor, defaulted move, deleted copy) common to every wrapper class in this header.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit AxisScaleOffset(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `detail::crossable<std::vector<detail::non_owning<ScaleOffset>>, &interface_type::getScaleOffsetAt, &interface_type::getNumScaleOffsets, &interface_type::setScaleOffsets> m_scaleOffsets` — per-axis scale-offset pairs indexed along the quantization axis.

### Friends
- `friend class Api`

## `class BlockEncoding : public qairt::ApiType<BlockEncoding, QairtQuantizeParams_BlockEncodingV1_t>`

`#include <QairtTensor.hpp>`

Block sizes and per-block scale-offset pairs for per-block scale-offset quantization. Construct directly: `BlockEncoding(const std::vector<uint32_t>& blockSize, const std::vector<ScaleOffset>& scaleOffsets)`. Attach to a `QuantizeParams` via `QuantizeParams::setBlockEncoding()`.

### Public Functions
```cpp
inline ~BlockEncoding()
BlockEncoding() noexcept = default
inline BlockEncoding(BlockEncoding &&other) noexcept
inline BlockEncoding &operator=(BlockEncoding &&other) noexcept
inline BlockEncoding(const BlockEncoding &other)
inline BlockEncoding &operator=(const BlockEncoding &other)
inline BlockEncoding shallowCopy() const
```
- `inline BlockEncoding(const std::vector<uint32_t> &blockSize, const std::vector<ScaleOffset> &scaleOffsets)` — construct a block encoding with the given block sizes and per-block scale-offset pairs. Parameters: `blockSize` [in] block sizes, one per quantization dimension; `scaleOffsets` [in] scale-offset pairs, one per quantization block.
- `inline std::vector<uint32_t> getBlockSizes() const` — get the block sizes along each quantization dimension. Returns vector of block sizes.
- `inline void setBlockSizes(const std::vector<uint32_t> &blockSizes) const` — set the block sizes along each quantization dimension. Parameters: `blockSizes` [in] block sizes, one per quantization dimension.
- `inline void setBlockSizes(std::vector<uint32_t> &&blockSizes) const` — see also `BlockEncoding::setBlockSizes(const std::vector<uint32_t>&)`.
- `inline std::vector<ScaleOffset> &getScaleOffsets()` — get the per-block scale-offset pairs for this encoding. Throws `qairt::Exception` on invalid handle. Returns reference to the vector of `ScaleOffset` objects, one per quantization block.
- `inline const std::vector<ScaleOffset> &getScaleOffsets() const` — const-access wrapper; see also `BlockEncoding::getScaleOffsets()`.
- `inline void setScaleOffsets(std::vector<ScaleOffset> scaleOffsets)` — set the per-block scale-offset pairs for this encoding. Parameters: `scaleOffsets` [in] scale-offset pairs, one per quantization block. Throws `qairt::Exception` on invalid handle.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BlockEncoding(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<uint32_t> m_blockSize` — block sizes along each quantization dimension.
- `detail::crossable<std::vector<detail::non_owning<ScaleOffset>>, &interface_type::getScaleOffsetAt, &interface_type::getNumScaleOffsets, &interface_type::setScaleOffsets> m_scaleOffsets` — scale-offset pairs for each quantization block.

## `class BlockwiseExpansion : public qairt::ApiType<BlockwiseExpansion, QairtQuantizeParams_BlockwiseExpansionV1_t>`

`#include <QairtTensor.hpp>`

Per-axis blockwise expansion quantization parameters, including axis, per-block scale-offset pairs, and block scale data. Construct directly: `BlockwiseExpansion(int32_t axis, const std::vector<ScaleOffset>& scaleOffsets, uint32_t numBlocksPerAxis, uint32_t blockScaleBitwidth)`. Supply block scale data as either 8-bit values via `setBlocksScale8()` or 16-bit values via `setBlocksScale16()` — these are mutually exclusive. Attach to a `QuantizeParams` via `QuantizeParams::setBlockwiseExpansion()`.

Note: setting 8-bit block scales clears any 16-bit block scales and vice versa.

### Public Functions
```cpp
inline ~BlockwiseExpansion()
BlockwiseExpansion() noexcept = default
inline BlockwiseExpansion(BlockwiseExpansion &&other) noexcept
inline BlockwiseExpansion &operator=(BlockwiseExpansion &&other) noexcept
inline BlockwiseExpansion(const BlockwiseExpansion &other)
inline BlockwiseExpansion &operator=(const BlockwiseExpansion &other)
inline BlockwiseExpansion shallowCopy() const
inline BlockwiseExpansion(int32_t axis, const std::vector<ScaleOffset> &ScaleOffsets, uint32_t numBlocksPerAxis, uint32_t blockScaleBitwidth)
inline int32_t getAxis() const
inline void setAxis(int32_t axis) const
inline uint32_t getNumBlocksPerAxis() const
inline void setNumBlocksPerAxis(uint32_t setNumBlocks) const
inline uint32_t getBlockScaleBitwidth() const
inline void setBlockScaleBitwidth(uint32_t setBlockScaleBw) const
inline std::vector<ScaleOffset> &getScaleOffsets()
inline const std::vector<ScaleOffset> &getScaleOffsets() const
inline void setScaleOffsets(const std::vector<ScaleOffset> &scaleOffsets)
inline BlockwiseExpansionBlockScaleStorageType getStorageType() const
inline const std::vector<uint8_t> &getBlocksScale8() const
inline const std::vector<uint16_t> &getBlocksScale16() const
inline void setBlocksScale8(const std::vector<uint8_t> &blocksScale8)
inline void setBlocksScale16(const std::vector<uint16_t> &blocksScale16)
```

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BlockwiseExpansion(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `detail::crossable<std::vector<detail::non_owning<ScaleOffset>>, &interface_type::getScaleOffsetAt, &interface_type::getNumScaleOffsets, &interface_type::setScaleOffsets> m_scaleOffsets` — scale-offset pairs for each quantization block in the expansion encoding.
- `mutable std::vector<uint8_t> m_blocksScale8` — block scale values stored as 8-bit data; mutually exclusive with `m_blocksScale16`.
- `mutable std::vector<uint16_t> m_blocksScale16` — block scale values stored as 16-bit data; mutually exclusive with `m_blocksScale8`.

## `class BwAxisScaleOffset : public qairt::ApiType<BwAxisScaleOffset, QairtQuantizeParams_BwAxisScaleOffsetV1_t>`

`#include <QairtTensor.hpp>`

Bit-width and per-axis float scales and integer offsets for bit-width per-axis scale-offset quantization. Construct directly: `BwAxisScaleOffset(uint32_t bitwidth, int32_t axis, const std::vector<float>& scales, const std::vector<int32_t>& offsets)`. Attach to a `QuantizeParams` via `QuantizeParams::setBwAxisScaleOffsetEncoding()`, or supply as the codebook descriptor for `VectorEncoding`.

### Public Functions
```cpp
inline ~BwAxisScaleOffset()
BwAxisScaleOffset() noexcept = default
inline BwAxisScaleOffset(BwAxisScaleOffset &&other) noexcept
inline BwAxisScaleOffset &operator=(BwAxisScaleOffset &&other) noexcept
inline BwAxisScaleOffset(const BwAxisScaleOffset &other)
inline BwAxisScaleOffset &operator=(const BwAxisScaleOffset &other)
inline BwAxisScaleOffset shallowCopy() const
```
- `inline BwAxisScaleOffset(uint32_t bitwidth, int32_t axis, const std::vector<float> &scales, const std::vector<int32_t> &offsets)` — construct a bit-width per-axis scale-offset encoding. Parameters: `bitwidth` [in] storage bit-width of the quantized values; `axis` [in] quantization axis index; `scales` [in] per-axis scale values, one per element along the quantization axis; `offsets` [in] per-axis integer zero-point offsets, one per axis element.
- `inline uint32_t getBitwidth() const` / `inline void setBitwidth(uint32_t bitwidth) const` — see also `QairtQuantizeParams_BwAxisScaleOffset_getBw` / `_setBw`.
- `inline int32_t getAxis() const` / `inline void setAxis(int32_t axis) const` — see also `QairtQuantizeParams_BwAxisScaleOffset_getAxis` / `_setAxis`.
- `inline const std::vector<float> &getScales() const` — get the per-axis scale values. Returns reference to the vector of scale values, one per element along the quantization axis.
- `inline void setScales(const std::vector<float> &scales)` — set the per-axis scale values. Parameters: `scales` [in] scale values, one per element along the quantization axis.
- `inline void setScales(std::vector<float> &&scales)` — see also `BwAxisScaleOffset::setScales(const std::vector<float>&)`.
- `inline const std::vector<int32_t> &getOffsets() const` — get the per-axis integer offset values. Returns reference to the vector of offset values, one per element along the quantization axis.
- `inline void setOffsets(const std::vector<int32_t> &offsets)` — set the per-axis integer offset values. Parameters: `offsets` [in] integer zero-point offsets, one per element along the quantization axis.
- `inline void setOffsets(std::vector<int32_t> &&offsets)` — see also `BwAxisScaleOffset::setOffsets(const std::vector<int32_t>&)`.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BwAxisScaleOffset(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<float> m_scales` — per-axis scale values, one per element along the quantization axis.
- `mutable std::vector<int32_t> m_offsets` — per-axis offset values, one per element along the quantization axis.

## `class BwAxisScaleOffsetMapped : public qairt::ApiType<BwAxisScaleOffsetMapped, QairtQuantizeParams_BwAxisScaleOffsetMappedV1_t>`

`#include <QairtTensor.hpp>`

Bit-width, per-axis scales and offsets, and a quantization mapping for bit-width per-axis scale-offset mapped quantization. Construct directly: `BwAxisScaleOffsetMapped()`. Supply scales via `setScales()`, offsets via `setOffsets()`, and a mapping scheme via `setMapping()`. Attach to a `QuantizeParams` via `QuantizeParams::setBwAxisScaleOffsetMappedEncoding()`.

### Public Functions
```cpp
inline ~BwAxisScaleOffsetMapped()
BwAxisScaleOffsetMapped() noexcept = default
inline BwAxisScaleOffsetMapped(BwAxisScaleOffsetMapped &&other) noexcept
inline BwAxisScaleOffsetMapped &operator=(BwAxisScaleOffsetMapped &&other) noexcept
inline BwAxisScaleOffsetMapped(const BwAxisScaleOffsetMapped &other)
inline BwAxisScaleOffsetMapped &operator=(const BwAxisScaleOffsetMapped &other)
inline BwAxisScaleOffsetMapped shallowCopy() const
```
- `inline uint32_t getBitwidth() const` / `inline void setBitwidth(uint32_t bitwidth) const` — see also `QairtQuantizeParams_BwAxisScaleOffsetMapped_getBw` / `_setBw`.
- `inline int32_t getAxis() const` / `inline void setAxis(int32_t axis) const` — see also `QairtQuantizeParams_BwAxisScaleOffsetMapped_getAxis` / `_setAxis`.
- `inline QuantizationEncodingMapping getMapping() const` / `inline void setMapping(QuantizationEncodingMapping mapping) const` — see also `QairtQuantizeParams_BwAxisScaleOffsetMapped_getMapping` / `_setMapping`.
- `inline const std::vector<float> &getScales() const` — get the per-axis scale values for this encoding. Returns reference to the vector of scale values, one per element along the quantization axis.
- `inline void setScales(const std::vector<float> &scales)` — set the per-axis scale values for this encoding. Parameters: `scales` [in] scale values, one per element along the quantization axis.
- `inline void setScales(std::vector<float> &&scales)` — see also `BwAxisScaleOffsetMapped::setScales(const std::vector<float>&)`.
- `inline const std::vector<int32_t> &getOffsets() const` — get the per-axis offset values for this encoding. Returns reference to the vector of offset values, one per element along the quantization axis.
- `inline void setOffsets(const std::vector<int32_t> &offsets)` — set the per-axis offset values for this encoding. Parameters: `offsets` [in] integer zero-point offsets, one per element along the quantization axis.
- `inline void setOffsets(std::vector<int32_t> &&offsets)` — see also `BwAxisScaleOffsetMapped::setOffsets(const std::vector<int32_t>&)`.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BwAxisScaleOffsetMapped(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<float> m_scales` — per-axis scale values for the mapped encoding, one per quantization axis element.
- `mutable std::vector<int32_t> m_offsets` — per-axis offset values for the mapped encoding, one per quantization axis element.

## `class BwBlockMapped : public qairt::ApiType<BwBlockMapped, QairtQuantizeParams_BwBlockMappedV1_t>`

`#include <QairtTensor.hpp>`

Bit-width, block sizes, a quantization mapping, and per-block scale-offset pairs for bit-width per-block mapped quantization. Construct directly: `BwBlockMapped()`. Supply block sizes via `setBlockSizes()`, a mapping scheme via `setMapping()`, and per-block `ScaleOffset` values via `setScaleOffsets()`. Attach to a `QuantizeParams` via `QuantizeParams::setBwBlockMapped()`.

### Public Functions
```cpp
inline ~BwBlockMapped()
BwBlockMapped() noexcept = default
inline BwBlockMapped(BwBlockMapped &&other) noexcept
inline BwBlockMapped &operator=(BwBlockMapped &&other) noexcept
inline BwBlockMapped(const BwBlockMapped &other)
inline BwBlockMapped &operator=(const BwBlockMapped &other)
inline BwBlockMapped shallowCopy() const
```
- `inline uint32_t getBitwidth() const` / `inline void setBitwidth(uint32_t bitwidth) const` — see also `QairtQuantizeParams_BwBlockMapped_getBw` / `_setBw`.
- `inline QuantizationEncodingMapping getMapping() const` / `inline void setMapping(QuantizationEncodingMapping mapping) const` — see also `QairtQuantizeParams_BwBlockMapped_getMapping` / `_setMapping`.
- `inline const std::vector<uint32_t> &getBlockSizes() const` — get the block sizes along each quantization dimension. Returns reference to the vector of block sizes.
- `inline void setBlockSizes(const std::vector<uint32_t> &blockSizes)` — set the block sizes along each quantization dimension. Parameters: `blockSizes` [in] block sizes, one per quantization dimension.
- `inline void setBlockSizes(std::vector<uint32_t> &&blockSizes)` — see also `BwBlockMapped::setBlockSizes(const std::vector<uint32_t>&)`.
- `inline void setBlockSize(std::vector<uint32_t> blockSizes)` — see also `BwBlockMapped::setBlockSizes(const std::vector<uint32_t>&)`.
- `inline std::vector<ScaleOffset> &getScaleOffsets()` — get the per-block scale-offset pairs for this encoding. Throws `qairt::Exception` on invalid handle. Returns reference to the vector of `ScaleOffset` objects, one per quantization block.
- `inline const std::vector<ScaleOffset> &getScaleOffsets() const` — const-access wrapper; see also `BwBlockMapped::getScaleOffsets()`.
- `inline void setScaleOffsets(std::vector<ScaleOffset> scaleOffsets)` — set the per-block scale-offset pairs for this encoding. Parameters: `scaleOffsets` [in] scale-offset pairs, one per quantization block. Throws `qairt::Exception` on invalid handle.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BwBlockMapped(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<uint32_t> m_blockSizes` — block sizes along each quantization dimension.
- `detail::crossable<std::vector<detail::non_owning<ScaleOffset>>, &interface_type::getScaleOffsetAt, &interface_type::getNumScaleOffsets, &interface_type::setScaleOffsets> m_scaleOffsets` — scale-offset pairs for each quantization block.

## `class BwBlockwiseExpansionMapped : public qairt::ApiType<BwBlockwiseExpansionMapped, QairtQuantizeParams_BwBlockwiseExpansionMappedV1_t>`

`#include <QairtTensor.hpp>`

Bit-width, per-axis block expansion parameters, a quantization mapping, and per-block scale data for bit-width blockwise expansion mapped quantization. Construct directly: `BwBlockwiseExpansionMapped()`. Supply block scale data as either 8-bit values via `setBlocksScale8()` or 16-bit values via `setBlocksScale16()` — these are mutually exclusive. Attach to a `QuantizeParams` via `QuantizeParams::setBwBlockwiseExpansionMapped()`.

Note: setting 8-bit block scales clears any 16-bit block scales and vice versa.

### Public Functions
```cpp
inline ~BwBlockwiseExpansionMapped()
BwBlockwiseExpansionMapped() noexcept = default
inline BwBlockwiseExpansionMapped(BwBlockwiseExpansionMapped &&other) noexcept
inline BwBlockwiseExpansionMapped &operator=(BwBlockwiseExpansionMapped &&other) noexcept
inline BwBlockwiseExpansionMapped(const BwBlockwiseExpansionMapped &other)
inline BwBlockwiseExpansionMapped &operator=(const BwBlockwiseExpansionMapped &other)
inline BwBlockwiseExpansionMapped shallowCopy() const
```
- `inline uint32_t getBitwidth() const` / `inline void setBitwidth(uint32_t bitwidth) const` — see also `QairtQuantizeParams_BwBlockwiseExpansionMapped_getBw` / `_setBw`.
- `inline QuantizationEncodingMapping getMapping() const` / `inline void setMapping(QuantizationEncodingMapping mapping) const` — see also `QairtQuantizeParams_BwBlockwiseExpansionMapped_getMapping` / `_setMapping`.
- `inline int32_t getAxis() const` / `inline void setAxis(int32_t axis) const` — see also `QairtQuantizeParams_BwBlockwiseExpansionMapped_getAxis` / `_setAxis`.
- `inline uint32_t getNumBlocksPerAxis() const` / `inline void setNumBlocksPerAxis(uint32_t numBlocksPerAxis) const` — see also `QairtQuantizeParams_BwBlockwiseExpansionMapped_getNumBlocksPerAxis` / `_setNumBlocksPerAxis`.
- `inline uint32_t getBlockScaleBitwidth() const` / `inline void setBlockScaleBitwidth(uint32_t blockScaleBitwidth) const` — see also `QairtQuantizeParams_BwBlockwiseExpansionMapped_getBlockScaleBitwidth` / `_setBlockScaleBitwidth`.
- `inline std::vector<ScaleOffset> &getScaleOffsets()` — get the per-block scale-offset pairs for this encoding. Throws `qairt::Exception` on invalid handle. Returns reference to the vector of `ScaleOffset` objects, one per quantization block.
- `inline const std::vector<ScaleOffset> &getScaleOffsets() const` — const-access wrapper; see also `BwBlockwiseExpansionMapped::getScaleOffsets()`.
- `inline void setScaleOffsets(const std::vector<ScaleOffset> &scaleOffsets)` — set the per-block scale-offset pairs for this encoding. Parameters: `scaleOffsets` [in] scale-offset pairs, one per quantization block. Throws `qairt::Exception` on invalid handle.
- `inline const std::vector<uint8_t> &getBlocksScale8() const` — get the 8-bit block scale values. Returns reference to the vector of 8-bit scale values. Note: valid only when block scales were set via `setBlocksScale8()`.
- `inline const std::vector<uint16_t> &getBlocksScale16() const` — get the 16-bit block scale values. Returns reference to the vector of 16-bit scale values. Note: valid only when block scales were set via `setBlocksScale16()`.
- `inline void setBlocksScale8(const std::vector<uint8_t> &blocksScale8)` — set the 8-bit block scale values. Clears any previously set 16-bit block scales. Parameters: `blocksScale8` [in] 8-bit block scale values, one per quantization block.
- `inline void setBlocksScale16(const std::vector<uint16_t> &blocksScale16)` — set the 16-bit block scale values. Clears any previously set 8-bit block scales. Parameters: `blocksScale16` [in] 16-bit block scale values, one per quantization block.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BwBlockwiseExpansionMapped(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `detail::crossable<std::vector<detail::non_owning<ScaleOffset>>, &interface_type::getScaleOffsetAt, &interface_type::getNumScaleOffsets, &interface_type::setScaleOffsets> m_scaleOffsets` — scale-offset pairs for each quantization block in the mapped encoding.
- `mutable std::vector<uint8_t> m_blocksScale8` — block scale values stored as 8-bit data; mutually exclusive with `m_blocksScale16`.
- `mutable std::vector<uint16_t> m_blocksScale16` — block scale values stored as 16-bit data; mutually exclusive with `m_blocksScale8`.

## `class BwFloatBlockEncoding : public qairt::ApiType<BwFloatBlockEncoding, QairtQuantizeParams_BwFloatBlockEncodingV1_t>`

`#include <QairtTensor.hpp>`

Bit-width and per-block floating-point scale-offset pairs for bit-width float block quantization. Construct directly: `BwFloatBlockEncoding()`. Supply block sizes via `setBlockSizes()` and one `FloatScaleOffset` per block via `setFloatScaleOffsets()`. Attach to a `QuantizeParams` via `QuantizeParams::setBwFloatBlockEncoding()`.

### Public Functions
```cpp
inline ~BwFloatBlockEncoding()
BwFloatBlockEncoding() noexcept = default
inline BwFloatBlockEncoding(BwFloatBlockEncoding &&other) noexcept
inline BwFloatBlockEncoding &operator=(BwFloatBlockEncoding &&other) noexcept
inline BwFloatBlockEncoding(const BwFloatBlockEncoding &other)
inline BwFloatBlockEncoding &operator=(const BwFloatBlockEncoding &other)
inline BwFloatBlockEncoding shallowCopy() const
```
- `inline uint32_t getBitwidth() const` / `inline void setBitwidth(uint32_t bitwidth) const` — see also `QairtQuantizeParams_BwFloatBlockEncoding_getBw` / `_setBw`.
- `inline const std::vector<uint32_t> &getBlockSizes() const` — get the block sizes along each quantization dimension. Returns reference to the vector of block sizes.
- `inline void setBlockSizes(const std::vector<uint32_t> &blockSizes)` — set the block sizes along each quantization dimension. Parameters: `blockSizes` [in] block sizes, one per quantization dimension.
- `inline void setBlockSizes(std::vector<uint32_t> &&blockSizes)` — see also `BwFloatBlockEncoding::setBlockSizes(const std::vector<uint32_t>&)`.
- `inline std::vector<FloatScaleOffset> &getFloatScaleOffsets()` — get the per-block floating-point scale-offset pairs. Throws `qairt::Exception` on invalid handle. Returns reference to the vector of `FloatScaleOffset` objects, one per quantization block.
- `inline const std::vector<FloatScaleOffset> &getFloatScaleOffsets() const` — const-access wrapper; see also `BwFloatBlockEncoding::getFloatScaleOffsets()`.
- `inline void setFloatScaleOffsets(std::vector<FloatScaleOffset> floatScaleOffsets)` — set the per-block floating-point scale-offset pairs. Parameters: `floatScaleOffsets` [in] float scale-offset pairs, one per quantization block. Throws `qairt::Exception` on invalid handle.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BwFloatBlockEncoding(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<uint32_t> m_blockSizes` — block size along each quantization dimension for the float block encoding.
- `detail::crossable<std::vector<detail::non_owning<FloatScaleOffset>>, &interface_type::getFloatScaleOffsetAt, &interface_type::getNumFloatScaleOffsets, &interface_type::setFloatScaleOffsets> m_floatScaleOffsets` — float scale-offset pairs, one per quantization block.

## `class BwScaleOffset : public qairt::ApiType<BwScaleOffset, QairtQuantizeParams_BwScaleOffsetV1_t>`

`#include <QairtTensor.hpp>`

Bit-width, scale, and integer offset for bit-width scale-offset quantization. Construct directly: `BwScaleOffset(uint32_t bitwidth, float scale, int32_t offset)`. Attach to a `QuantizeParams` via `QuantizeParams::setBwScaleOffsetEncoding()`.

### Public Functions
```cpp
inline ~BwScaleOffset()
BwScaleOffset() noexcept = default
inline BwScaleOffset(BwScaleOffset &&other) noexcept
inline BwScaleOffset &operator=(BwScaleOffset &&other) noexcept
inline BwScaleOffset(const BwScaleOffset &other)
inline BwScaleOffset &operator=(const BwScaleOffset &other)
inline BwScaleOffset shallowCopy() const
```
- `inline BwScaleOffset(uint32_t bitwidth, float scale, int32_t offset)` — construct a bit-width scale-offset encoding. Parameters: `bitwidth` [in] storage bit-width of the quantized values; `scale` [in] scale factor; `offset` [in] integer zero-point offset.
- `inline uint32_t getBitwidth() const` / `inline void setBitwidth(uint32_t bitwidth) const` — see also `QairtQuantizeParams_BwScaleOffset_getBw` / `_setBw`.
- `inline float getScale() const` / `inline void setScale(float scale) const` — see also `QairtQuantizeParams_BwScaleOffset_getScale` / `_setScale`.
- `inline int32_t getOffset() const` / `inline void setOffset(int32_t offset) const` — see also `QairtQuantizeParams_BwScaleOffset_getOffset` / `_setOffset`.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit BwScaleOffset(const std::shared_ptr<ApiTable> &apiTable)`

### Friends
- `friend class Api`

## `class ClientBuffer : public qairt::ApiType<ClientBuffer, QairtTensor_ClientBufferV1_t>`

`#include <QairtTensor.hpp>`

Raw memory descriptor for tensor data provided by the caller. Construct directly: `ClientBuffer()`. Associate with a `Tensor` via `TensorMemory::setClientBuffer()`.

### Public Functions
```cpp
ClientBuffer() noexcept = default
ClientBuffer(const ClientBuffer&) = delete
ClientBuffer &operator=(const ClientBuffer&) = delete
ClientBuffer(ClientBuffer&&) noexcept = default
ClientBuffer &operator=(ClientBuffer&&) noexcept = default
```
- `inline void *getData()` / `inline const void *getData() const` — see also `QairtTensor_ClientBuffer_getData`.
- `inline void setData(void *data)` — see also `QairtTensor_ClientBuffer_setData`.
- `inline uint32_t getDataSize() const` — see also `QairtTensor_ClientBuffer_getDataSize`.
- `inline void setDataSize(uint32_t dataSize)` — see also `QairtTensor_ClientBuffer_setDataSize`.

### Private Functions
- `inline explicit ClientBuffer(const std::shared_ptr<ApiTable> &apiTable)`
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`

### Private Members
- `friend Api`

### Friends
- `friend class TensorMemory`
- `friend class Tensor`

## `class FloatBlockEncoding : public qairt::ApiType<FloatBlockEncoding, QairtQuantizeParams_FloatBlockEncodingV1_t>`

`#include <QairtTensor.hpp>`

Per-block floating-point scale-offset pairs for float block quantization. Construct directly: `FloatBlockEncoding()`. Supply block sizes via `setBlockSizes()` and one `FloatScaleOffset` per block via `setFloatScaleOffsets()`. Attach to a `QuantizeParams` via `QuantizeParams::setFloatBlockEncoding()`.

### Public Functions
```cpp
inline ~FloatBlockEncoding()
FloatBlockEncoding() noexcept = default
inline FloatBlockEncoding(FloatBlockEncoding &&other) noexcept
inline FloatBlockEncoding &operator=(FloatBlockEncoding &&other) noexcept
inline FloatBlockEncoding(const FloatBlockEncoding &other)
inline FloatBlockEncoding &operator=(const FloatBlockEncoding &other)
inline FloatBlockEncoding shallowCopy() const
```
- `inline const std::vector<uint32_t> &getBlockSizes() const` — get the block sizes along each quantization dimension. Returns reference to the vector of block sizes.
- `inline void setBlockSizes(const std::vector<uint32_t> &blockSizes)` — set the block sizes along each quantization dimension. Parameters: `blockSizes` [in] block sizes, one per quantization dimension.
- `inline void setBlockSizes(std::vector<uint32_t> &&blockSizes)` — see also `FloatBlockEncoding::setBlockSizes(const std::vector<uint32_t>&)`.
- `inline std::vector<FloatScaleOffset> &getFloatScaleOffsets()` — get the per-block floating-point scale-offset pairs. Throws `qairt::Exception` on invalid handle. Returns reference to the vector of `FloatScaleOffset` objects, one per quantization block.
- `inline const std::vector<FloatScaleOffset> &getFloatScaleOffsets() const` — const-access wrapper; see also `FloatBlockEncoding::getFloatScaleOffsets()`.
- `inline void setFloatScaleOffsets(std::vector<FloatScaleOffset> floatScaleOffsets)` — set the per-block floating-point scale-offset pairs. Parameters: `floatScaleOffsets` [in] float scale-offset pairs, one per quantization block. Throws `qairt::Exception` on invalid handle.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit FloatBlockEncoding(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<uint32_t> m_blockSizes` — block size along each quantization dimension for the float block encoding.
- `detail::crossable<std::vector<detail::non_owning<FloatScaleOffset>>, &interface_type::getFloatScaleOffsetAt, &interface_type::getNumFloatScaleOffsets, &interface_type::setFloatScaleOffsets> m_floatScaleOffsets` — float scale-offset pairs, one per quantization block.

## `class FloatScaleOffset : public qairt::ApiType<FloatScaleOffset, QairtQuantizeParams_FloatScaleOffsetV1_t>`

`#include <QairtTensor.hpp>`

Floating-point scale and offset pair for float block quantization encodings. Construct directly: `FloatScaleOffset(float scale, float offset)`. Used as elements within `BwFloatBlockEncoding` and `FloatBlockEncoding`.

### Public Functions
```cpp
inline ~FloatScaleOffset()
FloatScaleOffset() noexcept = default
inline FloatScaleOffset(FloatScaleOffset &&other) noexcept
inline FloatScaleOffset &operator=(FloatScaleOffset &&other) noexcept
inline FloatScaleOffset(const FloatScaleOffset &other)
inline FloatScaleOffset &operator=(const FloatScaleOffset &other)
inline FloatScaleOffset shallowCopy() const
```
- `inline FloatScaleOffset(float scale, float offset)` — construct a floating-point scale-offset pair. Parameters: `scale` [in] scale factor; `offset` [in] floating-point zero-point offset.
- `inline float getScale()` / `inline float getScale() const` — see also `QairtQuantizeParams_FloatScaleOffset_getScale`.
- `inline void setScale(float scale) const` — see also `QairtQuantizeParams_FloatScaleOffset_setScale`.
- `inline float getOffset()` / `inline float getOffset() const` — see also `QairtQuantizeParams_FloatScaleOffset_getOffset`.
- `inline void setOffset(float offset) const` — see also `QairtQuantizeParams_FloatScaleOffset_setOffset`.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit FloatScaleOffset(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`

## `class Microscaling : public qairt::ApiType<Microscaling, QairtQuantizeParams_MicroscalingEncodingV1_t>`

`#include <QairtTensor.hpp>`

Microscaling (MX) quantization parameters, including float encoding, block dimensions, and per-block scale values. Construct directly: `Microscaling()`. Set the value encoding format via `setValueEncoding()`, block dimensions via `setBlockDimensions()`, and block scale data via `setBlockScales8()` (for Float8 scale type) or `setBlockScalesFloat()` (for Float16 or Float32 scale type). Attach to a `QuantizeParams` via `QuantizeParams::setMicroscalingEncoding()`.

Note: setting 8-bit block scales implicitly sets the scale data type to Float8; setting float block scales implicitly sets it to Float32.

### Public Functions
```cpp
inline ~Microscaling()
Microscaling() noexcept = default
inline Microscaling(Microscaling &&other) noexcept
inline Microscaling &operator=(Microscaling &&other) noexcept
inline Microscaling(const Microscaling &other)
inline Microscaling &operator=(const Microscaling &other)
inline Microscaling shallowCopy() const
```
- `inline FloatEncoding getValueEncoding() const` / `inline void setValueEncoding(FloatEncoding valueEncoding) const` — see also `QairtQuantizeParams_MicroscalingEncoding_getValueEncoding` / `_setValueEncoding`.
- `inline const std::vector<uint32_t> &getBlockDimensions() const` — get the block dimensions for this microscaling encoding. Returns reference to the vector of block dimension sizes, one per quantization dimension.
- `inline void setBlockDimensions(const std::vector<uint32_t> &blockDimensions)` — set the block dimensions for this microscaling encoding. Parameters: `blockDimensions` [in] block sizes, one per quantization dimension.
- `inline void setBlockDimensions(std::vector<uint32_t> &&blockDimensions)` — see also `Microscaling::setBlockDimensions(const std::vector<uint32_t>&)`.
- `inline size_t getBlockCount() const` — get the number of blocks in this microscaling encoding. Returns number of block scale values currently stored, or 0 if none have been set.
- `inline void setScaleDataType(DataType dtype) const` — set the data type used to store block scale values. Initializes the internal block-scale storage to match the given data type. `Float8` uses a `std::vector<uint8_t>` buffer; `Float16` and `Float32` use a `std::vector<float>` buffer. Calling this does not clear existing scale data unless the storage type changes. See also `QairtQuantizeParams_MicroscalingEncoding_setScaleDataType`. Parameters: `dtype` [in] data type for block scales, must be `Float8`, `Float16`, or `Float32`. Throws `qairt::Exception` on invalid handle.
- `inline DataType getScaleDataType() const` — get the data type used to store block scale values. See also `QairtQuantizeParams_MicroscalingEncoding_getScaleDataType`. Throws `qairt::Exception` on invalid handle. Returns the currently set scale data type.
- `inline const std::vector<uint8_t> &getBlockScales8() const` — get the 8-bit block scale values. Valid when the scale data type is `Float8`. Returns reference to the vector of 8-bit scale values.
- `inline void setBlockScales8(const std::vector<uint8_t> &blockScales8)` — set the 8-bit block scale values. Implicitly sets the scale data type to `Float8`. Replaces any previously set float block scales. Parameters: `blockScales8` [in] 8-bit block scale values, one per quantization block.
- `inline void setBlockScales8(std::vector<uint8_t> &&blockScales8)` — see also `Microscaling::setBlockScales8(const std::vector<uint8_t>&)`.
- `inline const std::vector<float> &getBlockScalesFloat() const` — get the floating-point block scale values. Valid when the scale data type is `Float16` or `Float32`. Returns reference to the vector of float scale values.
- `inline void setBlockScalesFloat(const std::vector<float> &blockScalesFloat)` — set the floating-point block scale values. Implicitly sets the scale data type to `Float32`. Replaces any previously set 8-bit block scales. Parameters: `blockScalesFloat` [in] float block scale values, one per quantization block.
- `inline void setBlockScalesFloat(std::vector<float> &&blockScalesFloat)` — see also `Microscaling::setBlockScalesFloat(const std::vector<float>&)`.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit Microscaling(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable std::vector<uint32_t> m_blockDimensions` — size of each quantization block along each dimension.
- `mutable std::variant<std::monostate, std::vector<uint8_t>, std::vector<float>> m_blockScales` — block scale values; holds `uint8_t` for `Float8` scale data type or `float` otherwise.

## `class QuantizeParams : public qairt::ApiType<QuantizeParams, QairtQuantizeParams_V1_t>`

`#include <QairtTensor.hpp>`

Container for the active quantization encoding applied to a tensor. Construct directly: `QuantizeParams()`. Set exactly one encoding variant by calling the corresponding setter (e.g., `setScaleOffsetEncoding()`, `setBwAxisScaleOffsetEncoding()`). Attach to a `Tensor` via `Tensor::setQuantizeParams()`. The active encoding type is identified at runtime via `getQuantizationEncoding()`.

### Public Functions
```cpp
inline ~QuantizeParams()
QuantizeParams() noexcept = default
inline QuantizeParams(QuantizeParams &&other) noexcept
inline QuantizeParams &operator=(QuantizeParams &&other) noexcept
inline QuantizeParams(const QuantizeParams &other)
inline QuantizeParams shallowCopy() const
inline QuantizeParams &operator=(const QuantizeParams &other)
inline QuantizationEncoding getQuantizationEncoding() const
inline void setScaleOffsetEncoding(ScaleOffset scaleOffset)
inline const ScaleOffset &getScaleOffsetEncoding() const
inline ScaleOffset &getScaleOffsetEncoding()
inline void setAxisScaleOffsetEncoding(AxisScaleOffset axisScaleOffset)
inline AxisScaleOffset &getAxisScaleOffsetEncoding()
inline const AxisScaleOffset &getAxisScaleOffsetEncoding() const
inline void setBwScaleOffsetEncoding(BwScaleOffset bwScaleOffset)
inline BwScaleOffset &getBwScaleOffsetEncoding()
inline const BwScaleOffset &getBwScaleOffsetEncoding() const
inline void setBwAxisScaleOffsetMappedEncoding(BwAxisScaleOffsetMapped bwAxisScaleOffsetMapped)
inline BwAxisScaleOffsetMapped &getBwAxisScaleOffsetMappedEncoding()
inline const BwAxisScaleOffsetMapped &getBwAxisScaleOffsetMappedEncoding() const
inline void setMicroscalingEncoding(Microscaling microscaling)
inline Microscaling &getMicroscalingEncoding()
inline const Microscaling &getMicroscalingEncoding() const
inline void setBwAxisScaleOffsetEncoding(BwAxisScaleOffset bwAxisScaleOffset)
inline BwAxisScaleOffset &getBwAxisScaleOffsetEncoding()
inline const BwAxisScaleOffset &getBwAxisScaleOffsetEncoding() const
inline void setBlockEncoding(BlockEncoding blockEncoding)
inline BlockEncoding &getBlockEncoding()
inline const BlockEncoding &getBlockEncoding() const
inline void setVectorEncoding(VectorEncoding vectorEncoding)
inline VectorEncoding &getVectorEncoding()
inline const VectorEncoding &getVectorEncoding() const
inline void setBlockwiseExpansion(BlockwiseExpansion blockwiseExpansion)
inline BlockwiseExpansion &getBlockwiseExpansion()
inline const BlockwiseExpansion &getBlockwiseExpansion() const
inline void setBwFloatBlockEncoding(BwFloatBlockEncoding bwFloatBlockEncoding)
inline BwFloatBlockEncoding &getBwFloatBlockEncoding()
inline const BwFloatBlockEncoding &getBwFloatBlockEncoding() const
inline void setFloatBlockEncoding(FloatBlockEncoding floatBlockEncoding)
inline FloatBlockEncoding &getFloatBlockEncoding()
inline const FloatBlockEncoding &getFloatBlockEncoding() const
inline void setBwBlockMapped(BwBlockMapped bwBlockMapped)
inline const BwBlockMapped &getBwBlockMapped() const
inline void setBwBlockwiseExpansionMapped(BwBlockwiseExpansionMapped bwBlockwiseExpansionMapped)
inline BwBlockwiseExpansionMapped &getBwBlockwiseExpansionMapped()
inline const BwBlockwiseExpansionMapped &getBwBlockwiseExpansionMapped() const
```

Each `set<Encoding>()` / `get<Encoding>()` pair activates and accesses exactly one alternative of the internal encoding variant (see Private Types below); setting a new encoding replaces whichever was previously active.

### Private Types
```cpp
using CrossableScaleOffset = detail::crossable<detail::non_owning<ScaleOffset>, &interface_type::getScaleOffset, &interface_type::setScaleOffset>
using CrossableAxisScaleOffset = detail::crossable<detail::non_owning<AxisScaleOffset>, &interface_type::getAxisScaleOffset, &interface_type::setAxisScaleOffset>
using CrossableBwScaleOffset = detail::crossable<detail::non_owning<BwScaleOffset>, &interface_type::getBwScaleOffset, &interface_type::setBwScaleOffset>
using CrossableBwAxisScaleOffsetMapped = detail::crossable<detail::non_owning<BwAxisScaleOffsetMapped>, &interface_type::getBwAxisScaleOffsetMapped, &interface_type::setBwAxisScaleOffsetMapped>
using CrossableMicroscaling = detail::crossable<detail::non_owning<Microscaling>, &interface_type::getMicroscalingEncoding, &interface_type::setMicroscalingEncoding>
using CrossableBwAxisScaleOffset = detail::crossable<detail::non_owning<BwAxisScaleOffset>, &interface_type::getBwAxisScaleOffset, &interface_type::setBwAxisScaleOffset>
using CrossableBlockEncoding = detail::crossable<detail::non_owning<BlockEncoding>, &interface_type::getBlockEncoding, &interface_type::setBlockEncoding>
using CrossableVectorEncoding = detail::crossable<detail::non_owning<VectorEncoding>, &interface_type::getVectorEncoding, &interface_type::setVectorEncoding>
using CrossableBlockwiseExpansion = detail::crossable<detail::non_owning<BlockwiseExpansion>, &interface_type::getBlockwiseExpansion, &interface_type::setBlockwiseExpansion>
using CrossableBwFloatBlockEncoding = detail::crossable<detail::non_owning<BwFloatBlockEncoding>, &interface_type::getBwFloatBlockEncoding, &interface_type::setBwFloatBlockEncoding>
using CrossableFloatBlockEncoding = detail::crossable<detail::non_owning<FloatBlockEncoding>, &interface_type::getFloatBlockEncoding, &interface_type::setFloatBlockEncoding>
using CrossableBwBlockMapped = detail::crossable<detail::non_owning<BwBlockMapped>, &interface_type::getBwBlockMapped, &interface_type::setBwBlockMapped>
using CrossableBwBlockwiseExpansionMapped = detail::crossable<detail::non_owning<BwBlockwiseExpansionMapped>, &interface_type::getBwBlockwiseExpansionMapped, &interface_type::setBwBlockwiseExpansionMapped>
```

### Private Functions
- `inline explicit QuantizeParams(const std::shared_ptr<ApiTable> &apiTable)`
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`

### Private Members
- `mutable std::variant<std::monostate, CrossableScaleOffset, CrossableAxisScaleOffset, CrossableBwScaleOffset, CrossableBwAxisScaleOffsetMapped, CrossableMicroscaling, CrossableBwAxisScaleOffset, CrossableBlockEncoding, CrossableVectorEncoding, CrossableBlockwiseExpansion, CrossableBwFloatBlockEncoding, CrossableFloatBlockEncoding, CrossableBwBlockMapped, CrossableBwBlockwiseExpansionMapped> m_encoding` — active quantization encoding; the variant alternative indicates the encoding type.

### Friends
- `friend class Api`

## `class ScaleOffset : public qairt::ApiType<ScaleOffset, QairtQuantizeParams_ScaleOffsetV1_t>`

`#include <QairtTensor.hpp>`

Per-tensor scale and integer offset for scale-offset quantization. Construct directly: `ScaleOffset(float scale, int32_t offset)`. Attach to a `QuantizeParams` via `QuantizeParams::setScaleOffsetEncoding()`.

### Public Functions
```cpp
inline ~ScaleOffset()
ScaleOffset() noexcept = default
inline ScaleOffset(ScaleOffset &&other) noexcept
inline ScaleOffset &operator=(ScaleOffset &&other) noexcept
inline ScaleOffset(const ScaleOffset &other)
inline ScaleOffset &operator=(const ScaleOffset &other)
inline ScaleOffset shallowCopy() const
```
- `inline ScaleOffset(float scale, int32_t offset)` — construct a scale-offset pair. Parameters: `scale` [in] scale factor; `offset` [in] integer zero-point offset.
- `inline float getScale() const` / `inline void setScale(float scale) const` — see also `QairtQuantizeParams_ScaleOffset_getScale` / `_setScale`.
- `inline int32_t getOffset() const` / `inline void setOffset(int32_t offset) const` — see also `QairtQuantizeParams_ScaleOffset_getOffset` / `_setOffset`.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit ScaleOffset(const std::shared_ptr<ApiTable> &apiTable)`

### Friends
- `friend class Api`

## `class Tensor : public qairt::ApiType<Tensor, QairtTensor_V1_t>`

`#include <QairtTensor.hpp>`

Descriptor for a single named tensor, including its shape, data type, memory binding, and quantization parameters. Obtained via `Graph` operations that return input or output tensors, or constructed directly: `Tensor()`. Set the shape via `setDimensions()`, the data type via `setDataType()`, a memory binding via `setTensorMemory()` or `setClientBuffer()`, and quantization parameters via `setQuantizeParams()`.

### Public Functions
```cpp
inline ~Tensor()
Tensor() = default
inline Tensor(const Tensor &other)
inline Tensor(Tensor &&other) noexcept
inline Tensor &operator=(Tensor &&other) noexcept
inline Tensor &operator=(const Tensor &other)
inline void setName(std::string str)
inline const std::string &getName() const
inline void setTensorProperties(const TensorProperties &tensorProperties)
inline TensorProperties &getTensorProperties()
inline const TensorProperties &getTensorProperties() const
inline void setDataFormat(uint32_t dataFormat)
inline uint32_t getDataFormat() const
inline void setDataType(DataType dataType)
inline DataType getDataType() const
inline QuantizeParams &getQuantizeParams()
inline const QuantizeParams &getQuantizeParams() const
inline void setQuantizeParams(const QuantizeParams &quantizeParams)
inline uint32_t getRank() const
inline void setDimensions(const std::vector<uint32_t> &dims)
inline void setDimensions(std::vector<uint32_t> &&dims)
inline const std::vector<uint32_t> &getDimensions() const
inline std::vector<uint32_t> &getDimensions()
inline TensorMemory &getTensorMemory()
inline const TensorMemory &getTensorMemory() const
inline void setTensorMemory(const TensorMemory &tensorMemory)
inline void setIsDynamicDimensions(const std::vector<bool> &isDynamicDims)
inline void setIsDynamicDimensions(const std::vector<detail::bool_wrapper> &isDynamicDims)
inline const std::vector<detail::bool_wrapper> &getIsDynamicDimensions() const
inline std::vector<detail::bool_wrapper> &getIsDynamicDimensions()
inline ClientBuffer &getClientBuffer()
inline const ClientBuffer &getClientBuffer() const
inline void setClientBuffer(const ClientBuffer &clientBuffer)
inline void setClientBuffer(RawBuffer &&buffer)
inline void setId(uint64_t id)
inline uint64_t getId() const
inline bool getIsProduced() const
inline Tensor shallowCopy() const
```

### Private Functions
- `inline Tensor(copy_table_tag_t, const Tensor &other)`
- `inline explicit Tensor(const std::shared_ptr<ApiTable> &apiTable)`
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`

### Private Members
- `friend Api`
- `detail::crossable<std::string, &interface_type::getName, &interface_type::setName> m_name` — tensor name used to identify this tensor within a graph.
- `detail::crossable<detail::non_owning<TensorProperties>, &interface_type::getTensorProperties, &interface_type::setTensorProperties> m_properties` — tensor attribute flags (input, output, static, optional, etc.).
- `detail::crossable<detail::non_owning<TensorMemory>, &interface_type::getTensorMemory, &interface_type::setTensorMemory> m_memory` — memory descriptor specifying how tensor data is stored or retrieved.
- `detail::crossable<detail::non_owning<QuantizeParams>, &interface_type::getQuantizeParams, &interface_type::setQuantizeParams> m_quantParams` — quantization parameters describing the encoding for this tensor's data.
- `mutable std::vector<uint32_t> m_dims` — shape of this tensor as a list of dimension sizes, ordered from outermost to innermost.
- `mutable std::vector<detail::bool_wrapper> m_isDynamicDims` — per-dimension dynamic flags; true indicates that dimension is dynamic at runtime.

### Friends
- `friend class ::qairt::ApiType`

## `class TensorMemory : public qairt::ApiType<TensorMemory, QairtTensor_MemoryV1_t>`

`#include <QairtTensor.hpp>`

Memory descriptor specifying how tensor data is stored or retrieved at runtime. Construct directly: `TensorMemory()`. Attach to a `Tensor` via `Tensor::setTensorMemory()`. The memory type (Raw, MemHandle, or RetrieveRaw) determines which accessor fields are active.

### Public Functions
```cpp
TensorMemory() noexcept = default
TensorMemory(const TensorMemory&) = delete
TensorMemory(TensorMemory&&) noexcept = default
TensorMemory &operator=(const TensorMemory&) = delete
TensorMemory &operator=(TensorMemory&&) noexcept = default
```
- `inline TensorMemType getMemoryType() const` — see also `QairtTensor_Memory_getMemoryType`.
- `inline ClientBuffer &getClientBuffer()` — get the client buffer associated with this tensor memory. Valid only when the memory type is Raw. The returned reference is bound to this `TensorMemory` and is invalidated if modification operations are performed on the same component API. See also `QairtTensor_Memory_getClientBuffer`. Throws `qairt::Exception` on invalid handle or memory type mismatch. Returns reference to the associated `ClientBuffer`.
- `inline const ClientBuffer &getClientBuffer() const` — const-access wrapper; see also `TensorMemory::getClientBuffer()`.
- `inline void setClientBuffer(const ClientBuffer &clientBuffer)` — set a client buffer on this tensor memory. Sets the memory type to Raw and associates the given client buffer. See also `QairtTensor_Memory_setClientBuffer`. Parameters: `clientBuffer` [in] the client buffer to associate with this tensor memory. Throws `qairt::Exception` on invalid handle.
- `inline void setClientBuffer(RawBuffer &&buffer)` — set a raw memory buffer on this tensor memory. Constructs a `ClientBuffer` from the given `RawBuffer` and associates it with this tensor memory. Falls back silently if the backend does not support `ClientBuffer` creation. Parameters: `buffer` [in] raw memory buffer whose data pointer and size are transferred into the new client buffer.
- `inline void setMemHandle(QairtMem_Handle_t memHandle)` — see also `QairtTensor_Memory_setMemHandle`.
- `inline QairtMem_Handle_t getMemHandle() const` — see also `QairtTensor_Memory_getMemHandle`.
- `inline void setRawRetrieveCallbacks(Qairt_GetTensorRawDataFn_t getCallback, Qairt_FreeTensorRawDataFn_t freeCallback, void *cookie)` — see also `QairtTensor_Memory_setRawRetrieveCallbacks`.
- `inline void getRawRetrieveCallbacks(Qairt_GetTensorRawDataFn_t *getCallback, Qairt_FreeTensorRawDataFn_t *freeCallback, void **cookie) const` — see also `QairtTensor_Memory_getRawRetrieveCallbacks`.

### Private Functions
- `inline explicit TensorMemory(const std::shared_ptr<ApiTable> &apiTable)`
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`

### Private Members
- `detail::crossable<detail::non_owning<ClientBuffer>, &interface_type::getClientBuffer, &interface_type::setClientBuffer> m_clientBuffer` — client buffer associated with this tensor memory when the memory type is Raw.

### Friends
- `friend class Api`

## `class TensorProperties : public qairt::ApiType<TensorProperties, QairtTensor_PropertiesV1_t>`

`#include <QairtTensor.hpp>`

Attribute flags describing the role and usage of a tensor within a graph. Construct directly: `TensorProperties()`. Attach to a `Tensor` via `Tensor::setTensorProperties()`.

### Public Functions
```cpp
TensorProperties() noexcept = default
TensorProperties(const TensorProperties&) = delete
TensorProperties(TensorProperties&&) noexcept = default
TensorProperties &operator=(const TensorProperties&) = delete
TensorProperties &operator=(TensorProperties&&) noexcept = default
```
- `inline void setIsInput(bool value)` / `inline bool isInput() const` — see also `QairtTensor_Properties_setIsInput` / `_getIsInput`.
- `inline void setIsOutput(bool value)` / `inline bool isOutput() const` — see also `QairtTensor_Properties_setIsOutput` / `_getIsOutput`.
- `inline void setIsNative(bool value)` / `inline bool isNative() const` — see also `QairtTensor_Properties_setIsNative` / `_getIsNative`.
- `inline void setIsNull(bool value)` / `inline bool isNull() const` — see also `QairtTensor_Properties_setIsNull` / `_getIsNull`.
- `inline void setIsStatic(bool value)` / `inline bool isStatic() const` — see also `QairtTensor_Properties_setIsStatic` / `_getIsStatic`.
- `inline void setIsOptional(bool value)` / `inline bool isOptional() const` — see also `QairtTensor_Properties_setIsOptional` / `_getIsOptional`.
- `inline void setIsUpdatable(bool value)` / `inline bool isUpdatable() const` — see also `QairtTensor_Properties_setIsUpdatable` / `_getIsUpdatable`.

### Private Functions
- `inline explicit TensorProperties(const std::shared_ptr<ApiTable> &apiTable)`

### Friends
- `friend class Api`

## `class VectorEncoding : public qairt::ApiType<VectorEncoding, QairtQuantizeParams_VectorEncodingV1_t>`

`#include <QairtTensor.hpp>`

Vector quantization (VQ) compression parameters, including a codebook descriptor and block-layout configuration. Construct directly: `VectorEncoding(const BwAxisScaleOffset& bwAxisScaleOffset, uint32_t rowsPerBlock, uint32_t colsPerBlock, uint32_t vectorStride, uint32_t vectorDimension, uint32_t indexBitwidth)`. Attach to a `QuantizeParams` via `QuantizeParams::setVectorEncoding()`.

### Public Functions
```cpp
inline ~VectorEncoding()
VectorEncoding() noexcept = default
inline VectorEncoding(VectorEncoding &&other) noexcept
inline VectorEncoding &operator=(VectorEncoding &&other) noexcept
inline VectorEncoding(const VectorEncoding &other)
inline VectorEncoding &operator=(const VectorEncoding &other)
inline VectorEncoding shallowCopy() const
```
- `inline VectorEncoding(const BwAxisScaleOffset &bwAxisScaleOffset, uint32_t rowsPerBlock, uint32_t colsPerBlock, uint32_t vectorStride, uint32_t vectorDimension, uint32_t indexBitwidth)` — construct a vector encoding with the given codebook and block layout. Parameters: `bwAxisScaleOffset` [in] bit-width per-axis scale-offset encoding for the codebook; `rowsPerBlock` [in] number of rows per block; `colsPerBlock` [in] number of columns per block; `vectorStride` [in] stride between vectors in the compressed representation; `vectorDimension` [in] dimension along which vectors are formed; `indexBitwidth` [in] bit-width of codebook indices.
- `inline uint32_t getRowsPerBlock() const` / `inline void setRowsPerBlock(uint32_t rowsPerBlock) const` — see also `QairtQuantizeParams_VectorEncoding_getRowsPerBlock` / `_setRowsPerBlock`.
- `inline uint32_t getColsPerBlock() const` / `inline void setColsPerBlock(uint32_t colsPerBlock) const` — see also `QairtQuantizeParams_VectorEncoding_getColsPerBlock` / `_setColsPerBlock`.
- `inline uint32_t getVectorStride() const` / `inline void setVectorStride(uint32_t vectorStride) const` — see also `QairtQuantizeParams_VectorEncoding_getVectorStride` / `_setVectorStride`.
- `inline uint32_t getVectorDimension() const` / `inline void setVectorDimension(uint32_t vectorDimension) const` — see also `QairtQuantizeParams_VectorEncoding_getVectorDimension` / `_setVectorDimension`.
- `inline uint32_t getIndexBw() const` / `inline void setIndexBw(uint32_t indexBw) const` — see also `QairtQuantizeParams_VectorEncoding_getIndexBw` / `_setIndexBw`.
- `inline const BwAxisScaleOffset &getBwAxisScaleOffset() const` — get the bit-width per-axis scale-offset encoding used for the vector codebook. Throws `qairt::Exception` on invalid handle. Returns const reference to the `BwAxisScaleOffset` codebook descriptor.
- `inline void setBwAxisScaleOffset(const BwAxisScaleOffset &bwAxisScaleOffset)` — set the bit-width per-axis scale-offset encoding used for the vector codebook. Parameters: `bwAxisScaleOffset` [in] codebook descriptor for the vector quantization. Throws `qairt::Exception` on invalid handle.

### Private Types
```cpp
using CrossableBwAxisScaleOffset = detail::crossable<detail::non_owning<BwAxisScaleOffset>, &interface_type::getBwAxisScaleOffset, &interface_type::setBwAxisScaleOffset>
```

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit VectorEncoding(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `mutable CrossableBwAxisScaleOffset m_bwAxisScaleOffset` — bit-width per-axis scale-offset encoding used for the vector codebook.

Last Published: Jul 02, 2026
