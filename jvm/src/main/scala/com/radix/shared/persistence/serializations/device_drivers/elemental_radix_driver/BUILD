package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "elemental-radix-driver-serializations",
    srcs = [
        "ElementalRadixDriverTypes.scala",
    ],
    resources = [],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/io/circe:circe_core",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/io/circe:circe_core",
    ],
)
