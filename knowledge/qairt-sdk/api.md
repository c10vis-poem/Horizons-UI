# QairtApi

> Extracted from the Qualcomm QAIRT SDK manual (`#QAIRT/` Drive capture). Official Qualcomm documentation — text extracted from the saved doc page; nav/boilerplate stripped.

Updated: Jul 02, 2026 80-63442-10 Rev: AL

Note: Some methods in this module are not yet implemented in the current release and will raise an exception if called. See the C API for full functionality.

`QairtApi.hpp` is the top-level convenience header that includes all other QAIRT C++ API headers. It does not define any symbols of its own.

To use the QAIRT C++ API, include this header in your application:

```
#include "QairtCppApi/QairtApi.hpp"
```

See the individual sections below for the full API reference.

## Exception

```
class Exception : public std::exception
```

Last Published: Jul 02, 2026
