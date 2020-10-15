package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "gi-uservice-serializations",
    srcs = [
        "actorInfo.scala",
        "protocol.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/gi:gi-serializations",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/algs/gi:gi-serializations",
    ],
)