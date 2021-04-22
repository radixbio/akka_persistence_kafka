package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "eve-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
        "eveState.scala",
        "eveUnit.scala",
    ],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/circe_custom_decoders:circe-custom-decoders",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "@third_party//3rdparty/jvm/io/circe:circe_literal",
    ],
    deps = [
        "//shared/circe_custom_decoders:circe-custom-decoders",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
        "@third_party//3rdparty/jvm/io/circe:circe_literal",
    ],
)
