package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "actor-discovery-serializations",
    srcs = [
        "Protocol.scala",
        "uservice/actorInfo.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
    ],
)
