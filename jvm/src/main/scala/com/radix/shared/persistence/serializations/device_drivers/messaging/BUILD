package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "messaging-driver-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = [
        "resources/reference.conf",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)
