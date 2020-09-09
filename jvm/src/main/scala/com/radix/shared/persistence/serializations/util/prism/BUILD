package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "prism-serialization",
    srcs = [
        "derivations.scala",
    ],
    plugins = ["@third_party//3rdparty/jvm/org/typelevel:kind_projector"],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//utils/prism/shared:prism-shared",
        "@third_party//3rdparty/jvm/com/codecommit:shims",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/squants:squants-serializations",
        "//utils/prism/shared:prism-shared",
        "@third_party//3rdparty/jvm/com/codecommit:shims",
    ],
)

scala_binary(
    name = "temp_serialization",
    srcs = ["derivations.scala"],
    main_class = "com.radix.shared.persistence.serializations.util.prism.MainTest",
    plugins = ["@third_party//3rdparty/jvm/org/typelevel:kind_projector"],
    deps = [
        ":prism-serialization",
    ],
)
