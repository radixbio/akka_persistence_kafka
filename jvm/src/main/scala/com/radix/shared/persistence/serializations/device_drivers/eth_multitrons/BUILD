package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "eth_multitrons-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
        "codex.scala"
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
         "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/elemental/gateway:elemental-gateway-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "@third_party//3rdparty/jvm/org/scodec:scodec_core",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/elemental/gateway:elemental-gateway-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "@third_party//3rdparty/jvm/org/scodec:scodec_core",
        "@third_party//3rdparty/jvm/org/typelevel:squants",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
    ],
)
