use anyhow::Result;
use std::time::Instant;
use wasmtime::{
    Config, Engine, Instance, Memory, Module, OptLevel, Store, Strategy, TypedFunc,
};

fn main() -> Result<()> {
    // ---- Configure engine to force Cranelift (so results are unambiguous) ----
    let mut cfg = Config::new();
    cfg.strategy(Strategy::Cranelift);        // force Cranelift
    cfg.cranelift_opt_level(OptLevel::Speed); // optional tuning
    //cfg.memory_guard_size(0); // disable guard pages if you trust code
    //cfg.memory_reservation_for_growth(0);
    // cfg.parallel_compilation(false);       // optional: disable parallel compilation if you want single-threaded timings
    let engine = Engine::new(&cfg)?;

    let wasm_path = "../toml2json/toml2json.wasm";

    // ---- Measure compilation (Wasm -> machine code) ----
    // All compilation completes before Module::from_file returns.
    let t0 = Instant::now();
    let module = Module::from_file(&engine, wasm_path)?;
    let t_compile = t0.elapsed();

    // ---- Measure instantiation (linking, memories/tables set up) ----
    let mut store = Store::new(&engine, ());
    let t1 = Instant::now();
    let instance = Instance::new(&mut store, &module, &[])?; // adjust imports if needed
    let t_instantiate = t1.elapsed();

    // ---- Get exports ----
    let memory: Memory = instance.get_memory(&mut store, "memory").expect("memory export");
    let allocate: TypedFunc<(i32, i32), i32> =
        instance.get_typed_func(&mut store, "allocate")?;
    let toml2json: TypedFunc<(i32, i32, i32, i32), i32> =
        instance.get_typed_func(&mut store, "toml2json")?;

    // ---- Prepare input and out-params ----
    let input = std::fs::read("../Cargo.lock")?;
    let in_len = i32::try_from(input.len())?;
    let in_ptr = allocate.call(&mut store, (in_len, 0))?;
    memory.write(&mut store, in_ptr as usize, &input)?;

    let out_ptr_ptr = allocate.call(&mut store, (4, 0))?;
    let out_len_ptr = allocate.call(&mut store, (4, 0))?;

    // ---- Measure the guest call only (execution) ----
    let t2 = Instant::now();
    let rc = toml2json.call(
        &mut store,
        (in_ptr, in_len, out_ptr_ptr, out_len_ptr),
    )?;
    anyhow::ensure!(rc == 0, "toml2json returned non-zero status {}", rc);
    let t_exec = t2.elapsed();

    // ---- Read results from memory (not counted in t_exec above) ----
    let mut buf = [0u8; 4];
    memory.read(&store, out_ptr_ptr as usize, &mut buf)?;
    let out_ptr = i32::from_le_bytes(buf) as usize;
    memory.read(&store, out_len_ptr as usize, &mut buf)?;
    let out_len = i32::from_le_bytes(buf) as usize;

    let mut output = vec![0u8; out_len];
    memory.read(&store, out_ptr, &mut output)?;
    println!("JSON: {}", String::from_utf8_lossy(&output));

    println!("compile = {:?}, instantiate = {:?}, execute = {:?}", t_compile, t_instantiate, t_exec);
    Ok(())
}
