package org.acme;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String... args) throws Exception {
        var cargoLock = Files.readAllBytes(
                Path.of(WasmResource.absoluteFile.substring(5))
                        .getParent()
                        .getParent()
                        .resolve("Cargo.lock"));

        var start = System.currentTimeMillis();

        var toml2Json =
                new Toml2Json(Toml2Json.Mode.BUILD_TIME_COMPILATION);
                // new Toml2Json(Toml2Json.Mode.RUNTIME_COMPILATION); // NO native-image
                // new Toml2Json(Toml2Json.Mode.INTERPRETER);

        System.out.println("instantiate " + timeSince(start));

        for (int i = 0; i < 5; i++) {
            start = System.currentTimeMillis();
            var out = toml2Json.convert(cargoLock);
            System.out.println(new String(out, 0, 20, StandardCharsets.UTF_8) + "...");
            System.out.println("convert time " + timeSince(start));
        }
    }

    private static long timeSince(long start) {
        return System.currentTimeMillis() - start;
    }
}
