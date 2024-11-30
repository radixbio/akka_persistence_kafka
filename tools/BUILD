load("@io_bazel_rules_scala//scala:providers.bzl", "declare_deps_provider")
load("@io_bazel_rules_scala//scala:scala_toolchain.bzl", "scala_toolchain")
load("//tools:modify_rules_scala_merge_jar_phase.bzl", "add_phase_merge_in_correct_order")

add_phase_merge_in_correct_order(
    name = "patch_phase_merge",
    visibility = ["//visibility:public"],
)
