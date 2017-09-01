
# 2.0.4 (PLANNED)

* Fix bug that resulted in 500 error when client sends broken headers

# 2.0.3

* Add ``httpStatus`` field to RestErrorCode
* Add SimpleRestErrorCode.
* Add ``jetty-rest-errors`` module with global jetty error registration capabilities

# 2.0.2

* Add more standard error codes (such as 429 Too Many Requests).
* Rename standard error code ``UNSUPPORTED`` to ``NOT_IMPLEMENTED`` to be consistent with the corresponding HTTP status.
* Remove dependency on servlet-api.

# 2.0.1

Initial version.