package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "opentrons-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//rainbow/jvm:urainbow-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/rainbow:rainbow-serialization",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
    deps = [
        "//rainbow/jvm:urainbow-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/utils/rainbow:rainbow-serialization",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
    ],
)
