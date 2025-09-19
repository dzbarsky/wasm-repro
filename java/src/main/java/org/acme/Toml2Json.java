package org.acme;

import com.dylibso.chicory.annotations.WasmModuleInterface;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.ByteArrayMemory;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.internal.CompilerInterpreterMachine;
import com.dylibso.chicory.wasm.Parser;

import java.nio.file.Path;

@WasmModuleInterface(WasmResource.absoluteFile)
public class Toml2Json {
    private Instance instance;
    private Toml2Json_ModuleExports exports;

    enum Mode {
        INTERPRETER,
        RUNTIME_COMPILATION,
        BUILD_TIME_COMPILATION
    }

    public Toml2Json() {
        this(Mode.BUILD_TIME_COMPILATION);
    }

    public Toml2Json(Mode mode) {
        var wasmPath = Path.of(WasmResource.absoluteFile.substring(5));
        switch (mode) {
            case INTERPRETER ->
                    instance = Instance.builder(Parser.parse(wasmPath))
                            .withMemoryFactory(ByteArrayMemory::new)
                            .build();
            case RUNTIME_COMPILATION ->
                    instance = Instance.builder(Parser.parse(wasmPath))
                            .withMachineFactory(MachineFactoryCompiler::compile)
                            .withMemoryFactory(ByteArrayMemory::new)
                            .build();
            case BUILD_TIME_COMPILATION ->
                    instance = Instance.builder(Toml2JsonModule.load())
                            .withMachineFactory(Toml2JsonModule::create)
                            .withMemoryFactory(ByteArrayMemory::new)
                            .build();
        }
        exports = new Toml2Json_ModuleExports(instance);
    }

    public byte[] convert(byte[] bytes) {
        var inLen = bytes.length;
        var inPtr = exports.allocate(inLen, 0);
        instance.memory().write(inPtr, bytes);

        var outPtr = exports.allocate(4, 0);
        var outLen = exports.allocate(4, 0);

        exports.toml2json(inPtr, inLen, outPtr, outLen);

        return instance.memory().readBytes(
                instance.memory().readInt(outPtr),
                instance.memory().readInt(outLen));
    }

}
