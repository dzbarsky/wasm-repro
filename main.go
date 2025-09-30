package main

import (
	"context"
	"encoding/binary"
	"os"
	"log"
	"time"

	"github.com/tetratelabs/wazero"
)

func main() {
	data, err := os.ReadFile("Cargo.lock")	
	if err != nil {
		log.Panicln(err)
	}

	ctx := context.Background()

	r := wazero.NewRuntime(ctx)

	wasm, err := os.ReadFile("toml2json/toml2json.wasm")
	if err != nil {
		log.Panicln(err)
	}

	start := time.Now()
	mod, err := r.Instantiate(ctx, wasm)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("instantiate", time.Since(start))

	for range 5 {
		start = time.Now()

		alloc := mod.ExportedFunction("allocate")
		mem := mod.Memory()
		res, err := alloc.Call(ctx, uint64(len(data)), 0)
		if err != nil || len(res) == 0 {
			log.Fatalf("alloc failed: %v", err)
		}
		ptr := uint32(res[0]) // guest pointer into linear memory
		if !mem.Write(ptr, data) {
			log.Fatalf("mem.Write out of bounds: ptr=%d size=%d", ptr, len(data))
		}

		outPtrSlotRes, err := alloc.Call(ctx, 4, 0)
		if err != nil || len(outPtrSlotRes) == 0 {
			log.Fatalf("alloc(outPtrSlot) failed: %v", err)
		}
		outPtrSlot := uint32(outPtrSlotRes[0])


		outLenSlotRes, err := alloc.Call(ctx, 4, 0)
		if err != nil || len(outLenSlotRes) == 0 {
			log.Fatalf("alloc(outLenSlot) failed: %v", err)
		}
		outLenSlot := uint32(outLenSlotRes[0])

		_, err = mod.ExportedFunction("toml2json").Call(
			ctx,
			uint64(ptr), uint64(len(data)),
			uint64(outPtrSlot), uint64(outLenSlot))
		if err != nil {
			log.Panicln(err)
		}
		outPtrBytes, ok := mem.Read(outPtrSlot, 4)
		if !ok {
			log.Fatalf("mem.Read outPtrSlot OOB")
		}
		outLenBytes, ok := mem.Read(outLenSlot, 4)
		if !ok {
			log.Fatalf("mem.Read outLenSlot OOB")
		}
		outPtr := binary.LittleEndian.Uint32(outPtrBytes)
		outLen := binary.LittleEndian.Uint32(outLenBytes)

		out, ok := mem.Read(outPtr, uint32(outLen))
		if !ok {
			log.Fatalf("mem.Read output OOB: ptr=%d len=%d", outPtr, outLen)
		}

		log.Println(string(out[:20]) + "...")
		log.Println("convert time", time.Since(start))
	}
}
