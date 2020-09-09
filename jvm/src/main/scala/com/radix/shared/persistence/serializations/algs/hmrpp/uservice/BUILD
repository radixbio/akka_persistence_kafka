package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "hmrpp-uservice-serializations",
    srcs = [
        "protocol.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/hmrpp:hmrpp-serializations",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/hmrpp:hmrpp-serializations",
    ],
)
