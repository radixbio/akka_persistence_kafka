package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "tecan-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/elemental/gateway:elemental-gateway-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "@third_party//3rdparty/jvm/io/7mind/izumi:logstage_core",
        "@third_party//3rdparty/jvm/io/circe:circe_core",
        "@third_party//3rdparty/jvm/org/scodec:scodec_core",
        "@third_party//3rdparty/jvm/com/lihaoyi:os_lib"
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/elemental/gateway:elemental-gateway-serializations",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "@third_party//3rdparty/jvm/io/7mind/izumi:logstage_core",
        "@third_party//3rdparty/jvm/io/circe:circe_core",
        "@third_party//3rdparty/jvm/org/scodec:scodec_core",
        "@third_party//3rdparty/jvm/com/lihaoyi:os_lib"
    ],
)
