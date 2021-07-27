package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "scheduler-actor-info",
    srcs = [
        "uservice/actorInfo.scala",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "//utils/scheduleruservice/jvm:scheduler-actor-protocol-lib",
    ],
    deps = [
        "//utils/scheduleruservice/jvm:scheduler-actor-protocol-lib",
    ],
)

scala_library(
    name = "scheduler-serialization",
    srcs = [
        "derivations.scala",
        "uservice/actorInfo.scala",
    ],
    plugins = ["//external:jar/org/typelevel/kind_projector_2_13_6"],
    resources = ["resources/reference.conf"],
    visibility = ["//visibility:public"],
    exports = [
        ":scheduler-actor-info",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
    deps = [
        ":scheduler-actor-info",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence:persistence-lib",
    ],
)
