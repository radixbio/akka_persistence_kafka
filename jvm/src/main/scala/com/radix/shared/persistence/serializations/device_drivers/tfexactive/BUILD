package(
    default_visibility = ["//shared/persistence:__subpackages__"],
)

scala_library(
    name = "tfexactive-serializations",
    srcs = [
        "actorInfo.scala",
        "defns.scala",
    ],
    resources = ["resources/reference.conf"],
    deps = [
        "@third_party//3rdparty/jvm/com/typesafe/akka:akka_actor_typed",
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
    ],
    exports = [
        "//shared/persistence/jvm/src/main/scala/com/radix/shared/persistence/serializations/device_drivers/common:driver-commons-serializations",
    ]
)
