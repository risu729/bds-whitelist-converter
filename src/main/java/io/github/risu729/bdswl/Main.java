/*
 * Copyright (c) 2023 Risu
 *
 *  This source code is licensed under the MIT license found in the
 *  LICENSE file in the root directory of this source tree.
 *
 */

package io.github.risu729.bdswl;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

final class Main {

  private static final Path WHITELIST = Path.of("whitelist.json");
  private static final Path ALLOWLIST = Path.of("allowlist.json");
  private static final Path PERMISSIONS = Path.of("permissions.json");
  private static final URI CAKES = URI.create("https://www.cxkes.me/xbox/xuid");

  @Contract(" -> fail")
  private Main() {
    throw new AssertionError();
  }

  @SuppressWarnings("ReassignedVariable")
  public static void main(@NotNull String @NotNull [] args) throws IOException {

    var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    Path allowlistPath;
    if (Files.exists(WHITELIST)) {
      allowlistPath = WHITELIST;
    } else if (Files.exists(ALLOWLIST)) {
      allowlistPath = ALLOWLIST;
    } else {
      throw new IllegalStateException("Neither whitelist.json nor allowlist.json found");
    }
    System.out.println("Found " + allowlistPath.getFileName());
    var allowlist = List.copyOf(gson.<List<Player>>fromJson(Files.readString(allowlistPath),
        new TypeToken<List<Player>>() {}.getType()));
    Files.delete(allowlistPath);

    if (allowlist.stream().anyMatch(player -> player.xuid() == null)) {
      System.out.printf(
          "Some players do not have XUIDs. Please visit %s and get XUIDs from their names.%n",
          CAKES);
      try (var scanner = new Scanner(System.in)) {
        allowlist = allowlist.stream().map(player -> {
          if (player.xuid() == null) {
            System.out.printf("Enter XUID(DEC) for %s:%n", player.name());
            return new Player(player.name(), scanner.nextLine(), player.ignoresPlayerLimit());
          } else {
            return player;
          }
        }).toList();
      }
    }

    Files.writeString(ALLOWLIST, gson.toJson(allowlist));
    System.out.printf("Exported %s%n", allowlistPath.getFileName());

    Files.writeString(PERMISSIONS,
        gson.toJson(allowlist.stream()
            .map(Player::xuid)
            .peek(Objects::requireNonNull)
            .map(xuid -> new Permission(PermissionLevel.OPERATOR, xuid))
            .toList()));
    System.out.printf("Exported %s%n", PERMISSIONS.getFileName());

    System.out.println("Done!");
  }

  private enum PermissionLevel {
    VISITOR,
    MEMBER,
    OPERATOR;

    @Override
    public @NotNull String toString() {
      return name().toLowerCase(Locale.ENGLISH);
    }
  }

  private record Player(@NotNull String name, @Nullable String xuid, boolean ignoresPlayerLimit) {}

  private record Permission(@NotNull PermissionLevel permission, @NotNull String xuid) {}
}
