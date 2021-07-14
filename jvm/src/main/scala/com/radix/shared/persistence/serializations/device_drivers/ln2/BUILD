package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "ln2-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
)
