package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "healthcheck-serialization",
    srcs = ["Serializers.scala"],
    plugins = ["@third_party//3rdparty/jvm/org/typelevel:kind_projector"],
    resources = ["resources/reference.conf"],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
    deps = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)
