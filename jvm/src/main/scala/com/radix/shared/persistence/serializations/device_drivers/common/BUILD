package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "driver-commons-serializations",
    srcs = [
        "reqresp.scala"
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib"
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib"
    ],
)
