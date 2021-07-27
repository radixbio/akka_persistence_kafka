package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "rainbow-actor-info",
    srcs = [
        "uservice/actorInfo.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "//utils/rainbowuservice/jvm:rainbow-actor-protocol-lib",
    ],
    deps = [
        "//utils/rainbowuservice/jvm:rainbow-actor-protocol-lib",
    ],
)

scala_library(
    name = "rainbow-serialization",
    srcs = [
        "derivations.scala",
        "uservice/actorInfo.scala",
    ],
    plugins = ["//external:jar/org/typelevel/kind_projector_2_13_6"],
    resources = ["resources/reference.conf"],
    visibility = ["//visibility:public"],
    exports = [
        ":rainbow-actor-info",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/org/typelevel:cats_core",
    ],
    deps = [
        ":rainbow-actor-info",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "@third_party//3rdparty/jvm/org/typelevel:cats_core",
    ],
)

scala_test(
    name = "rainbow-derivations-are-equivalent-to-gen",
    srcs = ["test/EquivalenceSpec.scala"],
    deps = [
        ":rainbow-serialization",
        "@third_party//3rdparty/jvm/com/lihaoyi:pprint",
    ],
)
