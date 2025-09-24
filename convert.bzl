def _impl(rctx):
    data = rctx.read(rctx.path(Label("//:Cargo.lock")))

    print("loading")
    toml2json = rctx.load_wasm(rctx.path(Label("//:toml2json/toml2json.wasm")))

    print("executing")
    result = rctx.execute_wasm(toml2json, "toml2json", input=data)
    if result.return_code != 0:
        fail(result.output)

    print(result.output[:20] + "...")
    print("done")

    print("executing")
    result = rctx.execute_wasm(toml2json, "toml2json", input=data)
    if result.return_code != 0:
        fail(result.output)

    print(result.output[:20] + "...")
    print("done")

convert = repository_rule(
    implementation = _impl,
)
