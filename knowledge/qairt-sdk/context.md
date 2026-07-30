# QairtContext

> Extracted from the Qualcomm QAIRT SDK manual (`#QAIRT/` Drive capture). Official Qualcomm documentation — text extracted from the saved doc page; nav/boilerplate stripped.

Updated: Jul 02, 2026 80-63442-10 Rev: AL

Note: Some methods in this module are not yet implemented in the current release and will raise an exception if called. See the C API for full functionality.

Include: `#include "QairtCppApi/QairtContext.hpp"`

C++ wrapper for the QAIRT context API. A `Backend` must be created before constructing a `Context` object. `namespace qairt`

## Enums

### `enum class ContextBinaryCompatibilityType : std::underlying_type_t<QairtContext_BinaryCompatibilityType_t>`

Binary compatibility policy used when loading a cached context binary.

| Enumerator | Description |
|---|---|
| `Permissive` | Binary is accepted if it can run on the device. Default policy. |
| `Strict` | Binary is accepted only if it fully utilizes hardware capability. |
| `Undefined` | Sentinel value; not a valid policy selection. |

Values map to `QAIRT_CONTEXT_BINARY_COMPATIBILITY_PERMISSIVE` / `_STRICT` / `QAIRT_CONTEXT_BINARY_COMPATIBILITY_TYPE_UNDEFINED`.

### `enum class ContextError : std::underlying_type_t<QairtContext_Error_t>`

Error codes returned by QAIRT context operations.

| Enumerator | Description |
|---|---|
| `NoError` | Operation succeeded. |
| `UnsupportedFeature` | An optional API feature is not supported by the backend. |
| `MemAlloc` | Memory allocation or deallocation failure. |
| `InvalidArgument` | An argument to the operation was invalid. |
| `CtxDoesNotExist` | The context has not yet been created in the backend. |
| `InvalidHandle` | The provided handle is not valid. |
| `NotFinalized` | Operation attempted before all graphs in the context were finalized. |
| `BinaryVersion` | The context binary has an incompatible version. |
| `CreateFromBinary` | Failed to create a context from a binary. |
| `GetBinarySizeFailed` | Failed to retrieve the size of the serialized context. |
| `GetBinaryFailed` | Failed to generate the serialized context. |
| `BinaryConfiguration` | The context binary configuration is invalid. |
| `SetProfile` | Failed to set profiling information. |
| `InvalidConfig` | One or more configuration values are invalid. |
| `BinarySuboptimal` | A suboptimal binary was used with strict compatibility mode. |
| `Aborted` | Call was aborted early due to a signal trigger. |
| `TimedOut` | Call was aborted early due to a signal timeout. |
| `IncrementInvalidBuffer` | The incremental binary buffer was not allocated by the backend. |
| `Undefined` | An undefined or unknown error occurred. |

Values map 1:1 to `QAIRT_CONTEXT_*` / `QAIRT_CONTEXT_ERROR_*` C constants (e.g. `NoError` = `QAIRT_CONTEXT_NO_ERROR`, `InvalidArgument` = `QAIRT_CONTEXT_ERROR_INVALID_ARGUMENT`, `Undefined` = `QAIRT_CONTEXT_ERROR_UNDEFINED`).

### `enum class ContextBinaryType : std::underlying_type_t<QairtContext_BinaryType_t>`

Storage format of a context binary.

| Enumerator | Description |
|---|---|
| `Raw` | Binary stored as a raw memory buffer. |
| `MemHandle` | Binary referenced via a memory handle. |
| `Undefined` | Sentinel value; not a valid binary type. |

Values map to `QAIRT_CONTEXT_BINARY_TYPE_RAW` / `_MEM_HANDLE` / `_UNDEFINED`.

### `enum class ContextSectionType : std::underlying_type_t<QairtContext_SectionType_t>`

Portion of the context binary targeted by section operations.

| Enumerator | Description |
|---|---|
| `Updatable` | Section containing all recent updates applied via tensor update APIs. |
| `UpdatableWeights` | Section containing recent static weight updates only. |
| `UpdatableQuantParams` | Section containing recent quantization parameter updates only. |
| `Undefined` | Sentinel value; not a valid section type. |

Values map to `QAIRT_CONTEXT_SECTION_UPDATABLE` / `_UPDATABLE_WEIGHTS` / `_UPDATABLE_QUANT_PARAMS` / `QAIRT_CONTEXT_SECTION_UNDEFINED`.

## `class Context : public qairt::ApiType<Context, QairtContext_V1_t>`

### Public Functions

- `Context() = default`
- `Context(const Context&) = delete`
- `Context(Context&&) noexcept = default`
- `Context &operator=(const Context&) = delete`
- `Context &operator=(Context&&) noexcept = default`
- `inline Context(const std::shared_ptr<ApiTable> &apiTable, QairtContext_Handle_t handle)`

**`inline void setConfig(const ContextConfiguration &config)`** — Set or modify configuration options on this context. Backends are not required to support reconfiguration after context creation. If the backend does not support the provided configuration, this call will fail. See also `QairtContext_setConfig`.
Parameters: `config` [in] context configuration object containing the options to apply.
Throws `qairt::Exception` on invalid handle, unsupported feature, or invalid configuration.

**`template<typename T> inline std::enable_if_t<std::is_base_of_v<ContextCustomConfiguration, T>> setConfiguration(const T &customConfigs)`** — Apply a collection of backend-specific custom configuration entries to this context.
Parameters: `customConfigs` [in] backend-specific custom configuration collection whose handles are applied individually to this context.
Throws `qairt::Exception` on invalid handle.

**`inline uint64_t getBinarySize() const`** — Get the size in bytes of the serialized binary representation of this context. All graphs in the context must be finalized before calling this method. Call `getBinary()` or `getBinary(void*, uint64_t)` after allocating a buffer of at least this size. See also `QairtContext_getBinarySize`.
Throws `qairt::Exception` on: invalid handle; unsupported feature; unfinalized graphs in the context; other retrieval failure.
Returns size in bytes required to hold the serialized context binary.

**`inline uint64_t getBinary(ContextBinaryBuffer &buffer)`** — Serialize this context into the provided binary buffer. All graphs in the context must be finalized before calling this method. Call `getBinarySize()` first to determine the required buffer size. The buffer's data pointer and size must be set before calling this method. See also `QairtContext_getBinary`.
Parameters: `buffer` [inout] pre-allocated binary buffer to receive the serialized context, whose size field must be at least `getBinarySize()` bytes.
Throws `qairt::Exception` on: invalid handle; unsupported feature; unfinalized graphs in the context; other serialization failure.
Returns number of bytes written into the buffer.

**`inline uint64_t getBinary(void *buffer, uint64_t bufferSize)`** — Wrapper which allows for serializing directly into a caller-managed raw memory buffer. See also `Context::getBinary(ContextBinaryBuffer&)`.

**`inline void updateContextTensors(const std::vector<Tensor*> &tensors)`** — Update the data and quantization parameters of previously created context tensors. Valid fields to update depend on tensor type:
- `UPDATEABLE_STATIC`: data and quantization parameters.
- `UPDATEABLE_NATIVE`, `UPDATEABLE_APP_READ`, `UPDATEABLE_APP_WRITE`, `UPDATEABLE_APP_READWRITE`: quantization parameters only.

Updates take effect only after `QairtGraph_finalize()` is called for one or more of the graphs to which the context tensors are associated. See also `QairtContext_updateContextTensors`.
Parameters: `tensors` [in] pointers to tensors to update, each must carry the ID assigned during tensor creation.
Throws `qairt::Exception` on: invalid context or tensor handle; NULL tensor handle array; incompatible tensor type; unsupported feature.

**`inline uint64_t getBinarySectionSize(const Graph &graph, ContextSectionType section) const`** — Get the size in bytes of a binary section for a specific graph. All graphs in the context must be finalized before calling this method. Use this to determine the buffer size needed before calling `getBinarySection()`. See also `QairtContext_getBinarySectionSize`.
Parameters: `graph` [in] graph whose binary section size is queried; `section` [in] portion of the context binary to query.
Throws `qairt::Exception` on: invalid handle; unsupported feature; unfinalized graphs in the context; other retrieval failure.
Returns size in bytes needed to hold the requested binary section.

**`inline uint64_t getBinarySection(const Graph &graph, ContextSectionType section, ContextBinaryBuffer &buffer, ApiTypeRef<const Profile&> profile, ApiTypeRef<const Signal&> signal)`** — Retrieve a section of the context binary for a specific graph. All graphs in the context must be finalized before calling this method. Call `getBinarySectionSize()` first to determine the required buffer size. The signal, if non-null, is considered in-use for the duration of this call. See also `QairtContext_getBinarySection`.
Parameters:
- `graph` [in] graph whose binary section is retrieved.
- `section` [in] portion of the context binary to retrieve.
- `buffer` [inout] pre-allocated binary buffer to receive the section, must be sized to at least `getBinarySectionSize()` bytes.
- `profile` [in] optional profile handle to collect metrics.
- `signal` [in] optional signal handle for controlling the operation.

Throws `qairt::Exception` on: invalid handle; unsupported feature; unfinalized graphs in the context; other serialization failure.
Returns number of bytes written into the buffer.

**`inline void applyBinarySection(const Graph &graph, ContextSectionType section, ContextBinaryBuffer &buffer, ApiTypeRef<const Profile&> profile, ApiTypeRef<const Signal&> signal)`** — Apply a previously retrieved binary section to this context. See also `QairtContext_applyBinarySection`.
Parameters:
- `graph` [in] graph to which the binary section applies.
- `section` [in] portion of the context binary being applied.
- `buffer` [in] binary buffer containing the section to apply. When persistent binary mode is enabled, this buffer must remain valid through context teardown.
- `profile` [in] optional profile handle to collect metrics.
- `signal` [in] optional signal handle for controlling the operation.

Throws `qairt::Exception` on: invalid handle; unsupported feature; memory allocation failure; profiling error.

**`inline Graph createGraph(const char *graphName, ApiTypeRef<const qairt::GraphConfiguration&> graphConfiguration)`** — Create a new graph within this context. See also `QairtContext_createGraph`.
Parameters: `graphName` [in] unique null-terminated identifier for the graph within this context; `graphConfiguration` [in] configuration options for the graph, optional.
Throws `qairt::Exception` on: invalid context handle; NULL or duplicate graph name; memory or resource allocation failure; unsupported configuration options.
Returns the newly created `Graph` object.

**`inline std::shared_ptr<Graph> retrieveGraph(const char *graphName)`** — See also `Context::retrieveGraph(const std::string&)`.

**`inline std::shared_ptr<Graph> retrieveGraph(const std::string &graphName)`** — Retrieve an existing graph from this context by name. See also `QairtContext_retrieveGraph`.
Parameters: `graphName` [in] name of the graph to retrieve.
Throws `qairt::Exception` on: invalid context handle; NULL or invalid graph name; no graph with the specified name exists in this context; memory allocation failure.
Returns shared pointer to the retrieved `Graph` object.

**`inline void setFreeProfile(Profile &profile)`** — Set the profile handle used to collect metrics during context teardown. See also `QairtContext_free`.
Parameters: `profile` [in] profile object to populate during context teardown.

- `template<typename T, typename U, typename V> inline ApiType(const ApiType<T, U, V> &parent, detail::non_owning_handle<handle_type> noh)`
- `inline explicit ApiType(const std::shared_ptr<T_Table> &apiTable)`
- `inline ApiType(copy_table_tag_t, const ApiType &other)`
- `ApiType() noexcept = default`
- `ApiType(const ApiType&) = delete`
- `ApiType(ApiType&&) noexcept = default`
- `ApiType &operator=(const ApiType&) = delete`
- `ApiType &operator=(ApiType&&) noexcept = default`

### Private Functions
- `inline Context(const std::shared_ptr<ApiTable> &apiTable, QairtBackend_Handle_t backendHandle, QairtDevice_Handle_t deviceHandle, QairtContext_ConfigHandle_t contextConfigHandle)`
- `inline Context(const std::shared_ptr<ApiTable> &apiTable, QairtBackend_Handle_t backendHandle, QairtDevice_Handle_t deviceHandle, QairtContext_ConfigHandle_t contextConfigHandle, QairtContext_BinaryBufferHandle_t binaryBufferHandle, QairtSignal_Handle_t signalHandle = nullptr, QairtProfile_Handle_t profileHandle = nullptr)`
- `inline void customFree(QairtContext_Handle_t handle) const`

### Private Members
- `friend Api`
- `QairtProfile_Handle_t m_freeProfileHandle = nullptr` — Profile handle used to collect metrics during context teardown.

### Friends
- `friend class ::qairt::ApiType`

## `class ContextAsyncExecutionQueueDepth : public qairt::ApiType<ContextAsyncExecutionQueueDepth, QairtContext_AsyncExecutionDepthV1_t>`

`#include <QairtContext.hpp>`

Queue depth configuration for asynchronous context execution.

### Public Functions

- `ContextAsyncExecutionQueueDepth() noexcept = default`
- `ContextAsyncExecutionQueueDepth(ContextAsyncExecutionQueueDepth&&) noexcept = default`
- `ContextAsyncExecutionQueueDepth &operator=(ContextAsyncExecutionQueueDepth&&) noexcept = default`

**`inline uint32_t getDepth() const`** — Get the current queue depth for asynchronous execution. See also `QairtContext_AsyncExecutionGetDepth`.
Throws `qairt::Exception` on invalid handle. Returns maximum number of outstanding asynchronous execution requests.

**`inline void setDepth(uint32_t depth)`** — Set the queue depth for asynchronous execution. See also `QairtContext_AsyncExecutionSetDepth`.
Parameters: `depth` [in] maximum number of outstanding asynchronous execution requests.
Throws `qairt::Exception` on invalid handle or invalid argument.

### Private Functions
- `inline explicit ContextAsyncExecutionQueueDepth(const std::shared_ptr<ApiTable> &apiTable)`

### Friends
- `friend class Api`

## `class ContextBinary : public qairt::ApiType<ContextBinary, QairtContext_BinaryV1_t>`

`#include <QairtContext.hpp>`

Descriptor pairing a binary type with its associated buffer for context serialization.

### Public Functions

- `ContextBinary() noexcept = default`
- `ContextBinary(ContextBinary&&) noexcept = default`
- `ContextBinary &operator=(ContextBinary&&) noexcept = default`

**`inline ContextBinaryType getType() const`** — Get the storage format type of this context binary. See also `QairtContext_binaryGetType`.
Throws `qairt::Exception` on invalid handle. Returns storage format of this binary (e.g., Raw or MemHandle).

**`inline ContextBinaryBuffer &getBuffer()`** — Get the binary buffer associated with this context binary. See also `QairtContext_binaryGetBuffer`.
Throws `qairt::Exception` on invalid handle. Returns reference to the associated `ContextBinaryBuffer`.

**`inline const ContextBinaryBuffer &getBuffer() const`** — Get the binary buffer associated with this context binary. See also `QairtContext_binaryGetBuffer`.
Throws `qairt::Exception` on invalid handle. Returns const reference to the associated `ContextBinaryBuffer`.

**`inline void setBuffer(ContextBinaryBuffer &&buffer)`** — Set the binary buffer for this context binary. See also `QairtContext_binarySetBuffer`.
Parameters: `buffer` [in] binary buffer to associate with this context binary object.
Throws `qairt::Exception` on invalid handle.

### Private Members
- `friend Api`
- `detail::crossable<detail::non_owning<ContextBinaryBuffer>, &interface_type::getBuffer, &interface_type::setBuffer> m_buffer` — Binary buffer associated with this context binary object.

## `class ContextBinaryBuffer : public qairt::ApiType<ContextBinaryBuffer, QairtContext_BinaryBufferV1_t>`

`#include <QairtContext.hpp>`

Buffer descriptor for a serialized context binary. Obtained via `Api::make<ContextBinaryBuffer>()`.

### Public Functions

- `ContextBinaryBuffer() noexcept = default`
- `ContextBinaryBuffer(ContextBinaryBuffer&&) noexcept = default`
- `ContextBinaryBuffer &operator=(ContextBinaryBuffer&&) noexcept = default`

**`inline void *getData()`** — Get the raw data pointer stored in this buffer. See also `QairtContext_BinaryBuffer_getData`.
Throws `qairt::Exception` on invalid handle. Returns pointer to the underlying buffer memory, or nullptr if none has been set.

**`inline const void *getData() const`** — Get the raw data pointer stored in this buffer. See also `QairtContext_BinaryBuffer_getData`.
Throws `qairt::Exception` on invalid handle. Returns pointer to the underlying buffer memory, or nullptr if none has been set.

**`inline void setData(void *data)`** — Set the raw data pointer for this buffer. See also `QairtContext_BinaryBuffer_setData`.
Parameters: `data` [in] pointer to the memory region to associate with this buffer.
Throws `qairt::Exception` on invalid handle.

**`inline uint64_t getSize() const`** — Get the size of this buffer in bytes. See also `QairtContext_BinaryBuffer_getSize`.
Throws `qairt::Exception` on invalid handle. Returns buffer size in bytes, or 0 if no size has been set.

**`inline void setSize(uint64_t size) const`** — Set the size of this buffer in bytes. See also `QairtContext_BinaryBuffer_setSize`.
Parameters: `size` [in] size in bytes of the memory region referenced by this buffer.
Throws `qairt::Exception` on invalid handle.

### Private Functions
- `inline explicit ContextBinaryBuffer(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`

### Friends
- `friend class ::qairt::ApiType`

## `class ContextConfiguration : public qairt::ApiType<ContextConfiguration, QairtContext_ConfigV1_t>`

`#include <QairtContext.hpp>`

Configuration object for context creation and reconfiguration.

### Public Functions

- `ContextConfiguration() noexcept = default`
- `ContextConfiguration(ContextConfiguration&&) noexcept = default`
- `ContextConfiguration &operator=(ContextConfiguration&&) noexcept = default`

**`inline void setPriority(Priority p)`** — Set the scheduling priority for this context configuration. See also `QairtContext_Config_setPriority`.
Parameters: `p` [in] desired execution priority.
Throws `qairt::Exception` on invalid handle or invalid argument.

**`inline Priority getPriority() const`** — Get the scheduling priority for this context configuration. See also `QairtContext_Config_getPriority`.
Throws `qairt::Exception` on invalid handle. Returns current execution priority.

**`inline std::string &getOemKey()`** — Get the Original Equipment Manufacturer (OEM) key string for this context configuration. See also `QairtContext_Config_getOemKey`.
Throws `qairt::Exception` on invalid handle. Returns reference to the OEM authentication key string. Empty if not set.

**`inline const std::string &getOemKey() const`** — Get the Original Equipment Manufacturer (OEM) key string for this context configuration. See also `QairtContext_Config_getOemKey`.
Throws `qairt::Exception` on invalid handle. Returns const reference to the OEM authentication key string. Empty if not set.

**`inline void setOemKey(std::string &&oemKey)`** — Set the Original Equipment Manufacturer (OEM) key string for this context configuration. See also `QairtContext_Config_setOemKey`.
Parameters: `oemKey` [in] OEM authentication key string to set.
Throws `qairt::Exception` on invalid handle.

**`inline void getOemKey(const std::string &oemKey)`**

**`inline void setAsyncExecutionQueueDepth(const ContextAsyncExecutionQueueDepth &aed)`** — Set the asynchronous execution queue depth for this context configuration. See also `QairtContext_Config_setAsyncQueueDepth`.
Parameters: `aed` [in] async execution queue depth configuration object.
Throws `qairt::Exception` on invalid handle.

**`inline ContextAsyncExecutionQueueDepth &getAsyncExecutionQueueDepth()`** — Get the asynchronous execution queue depth configuration for this context. See also `QairtContext_Config_getAsyncQueueDepth`.
Throws `qairt::Exception` on invalid handle. Returns reference to the async execution queue depth object.

**`inline const ContextAsyncExecutionQueueDepth &getAsyncExecutionQueueDepth() const`** — Get the asynchronous execution queue depth configuration for this context. See also `QairtContext_Config_getAsyncQueueDepth`.
Throws `qairt::Exception` on invalid handle. Returns const reference to the async execution queue depth object.

**`inline ContextConfiguration &setCustomConfig(const ContextCustomConfig &config)`** — Set a single backend-specific custom configuration entry on this context configuration. See also `QairtContext_Config_setCustomConfigs`.
Parameters: `config` [in] backend-specific custom configuration entry to apply.
Throws `qairt::Exception` on invalid handle. Returns reference to this configuration object, allowing method chaining.

**`inline ContextConfiguration &setCustomConfigs(const ContextCustomConfiguration &config)`** — Set a collection of backend-specific custom configuration entries on this context configuration. See also `QairtContext_Config_setCustomConfigs`.
Parameters: `config` [in] collection of backend-specific custom configuration entries to apply.
Throws `qairt::Exception` on invalid handle. Returns reference to this configuration object, allowing method chaining.

**`inline std::vector<std::string> &getEnableGraphs()`** — Get the list of graph names selectively enabled for this context configuration. See also `QairtContext_Config_getNumEnabledGraphs`.
Throws `qairt::Exception` on invalid handle. Returns reference to the vector of enabled graph name strings.

**`inline const std::vector<std::string> &getEnableGraphs() const`** — Get the list of graph names selectively enabled for this context configuration. See also `QairtContext_Config_getNumEnabledGraphs`.
Throws `qairt::Exception` on invalid handle. Returns const reference to the vector of enabled graph name strings.

**`inline void setEnableGraphs(std::vector<std::string> enabledGraphs)`** — Set the list of graph names selectively enabled for this context configuration. See also `QairtContext_Config_setEnableGraph`.
Parameters: `enabledGraphs` [in] names of graphs to enable.
Throws `qairt::Exception` on invalid handle.

**`inline void setMemoryLimitHint(uint64_t limit)`** — Set a hint on the maximum memory the backend should use for this context. This is advisory only; the backend may exceed the limit if required. See also `QairtContext_Config_setMemoryLimitHint`.
Parameters: `limit` [in] memory limit hint in bytes.
Throws `qairt::Exception` on invalid handle or invalid argument.

**`inline uint64_t getMemoryLimitHint() const`** — Get the memory limit hint for this context configuration. See also `QairtContext_Config_getMemoryLimitHint`.
Throws `qairt::Exception` on invalid handle. Returns memory limit hint in bytes, or 0 if no limit has been set.

**`inline void setIsPersistentBinary(bool isPersistentBinary)`** — Set whether the context binary should be treated as persistent. See also `QairtContext_Config_setIsPersistentBinary`.
Parameters: `isPersistentBinary` [in] true to enable persistent binary mode; false to disable.
Throws `qairt::Exception` on invalid handle or invalid argument.

**`inline bool getIsPersistentBinary() const`** — Get whether the context binary is configured as persistent. See also `QairtContext_Config_getIsPersistentBinary`.
Throws `qairt::Exception` on invalid handle. Returns true if persistent binary mode is enabled; false otherwise.

**`inline void setBinaryCompatibilityType(ContextBinaryCompatibilityType bct)`** — Set the binary compatibility policy for loading cached context binaries. See also `QairtContext_Config_setBinaryCompatibilityType`.
Parameters: `bct` [in] binary compatibility policy to enforce.
Throws `qairt::Exception` on invalid handle or invalid argument.

**`inline ContextBinaryCompatibilityType getBinaryCompatibilityType() const`** — Get the binary compatibility policy for loading cached context binaries. See also `QairtContext_Config_getBinaryCompatibilityType`.
Throws `qairt::Exception` on invalid handle. Returns current binary compatibility policy.

### Private Functions
- `inline void prepareToCross() const`
- `inline void updateAfterCross() const`
- `inline explicit ContextConfiguration(const std::shared_ptr<ApiTable> &apiTable)`

### Private Members
- `friend Api`
- `detail::crossable<std::string, &interface_type::getOemKey, &interface_type::setOemKey> m_oemKey` — Original Equipment Manufacturer (OEM) key string for backend authentication.
- `detail::crossable<detail::non_owning<ContextAsyncExecutionQueueDepth>, &interface_type::getAsyncQueueDepth, &interface_type::setAsyncQueueDepth> m_depth` — Maximum number of outstanding asynchronous execution requests.
- `mutable std::vector<std::string> m_enabledGraphs` — Names of graphs selectively enabled for this context configuration.

## `class ContextCustomConfig : public qairt::CustomConfigType`

`#include <QairtContext.hpp>`

Abstract base class for a single backend-specific context custom configuration entry.

### Public Functions
- `virtual ~ContextCustomConfig() = default`
- `virtual QairtContext_CustomConfigHandle_t getCustomConfigHandle() const = 0`

### Protected Functions
- `ContextCustomConfig() = default`
- `ContextCustomConfig(const ContextCustomConfig&) = default`
- `ContextCustomConfig(ContextCustomConfig&&) noexcept = default`
- `ContextCustomConfig &operator=(const ContextCustomConfig&) = default`
- `ContextCustomConfig &operator=(ContextCustomConfig&&) noexcept = default`

## `class ContextCustomConfiguration`

`#include <QairtContext.hpp>`

Abstract base class for a collection of backend-specific context custom configuration entries.

### Public Functions
- `virtual ~ContextCustomConfiguration() = default`
- `virtual std::vector<QairtContext_CustomConfigHandle_t> getCustomConfigs() const = 0`

### Protected Functions
- `ContextCustomConfiguration() = default`
- `ContextCustomConfiguration(const ContextCustomConfiguration&) = default`
- `ContextCustomConfiguration(ContextCustomConfiguration&&) noexcept = default`
- `ContextCustomConfiguration &operator=(const ContextCustomConfiguration&) = default`
- `ContextCustomConfiguration &operator=(ContextCustomConfiguration&&) noexcept = default`

Last Published: Jul 02, 2026
