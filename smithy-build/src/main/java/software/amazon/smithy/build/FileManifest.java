/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.Node;

/**
 * Creates and tracks the files generated by a {@link SmithyBuildPlugin}.
 *
 * Mutating FileManifest implementations MUST be thread-safe as they can
 * be used concurrently across multiple threads when building models.
 */
public interface FileManifest {
    /**
     * Create a default file manifest for the given base path.
     *
     * @param basePath Base path where files are written.
     * @return Returns the created manifest.
     */
    static FileManifest create(Path basePath) {
        return new DefaultFileManifest(basePath);
    }

    /**
     * Gets the base directory of the manifest.
     *
     * @return Returns the base directory.
     */
    Path getBaseDir();

    /**
     * Gets all of the files in the result.
     *
     * <p>The order of files returned should be stable across calls.
     *
     * @return Returns the files in the manifest.
     */
    Set<Path> getFiles();

    /**
     * Adds a path to the manifest.
     *
     * <p>The given path must be relative or within the base directory.
     *
     * @param path Path to add.
     * @return Returns the path resolved against any base URL.
     */
    @SuppressWarnings("unused")
    Path addFile(Path path);

    /**
     * Adds the files from another FileManifest into this FileManifest.
     *
     * @param manifest Other object to merge with.
     */
    default void addAllFiles(FileManifest manifest) {
        manifest.getFiles().forEach(this::addFile);
    }

    /**
     * Resolves a path against the base path of the manifest.
     *
     * @param path Path to resolve against the base URL.
     * @return Returns the resolved, absolute path.
     * @throws SmithyBuildException if the resolved path cannot
     *   falls outside of the base URL of the manifest.
     */
    default Path resolvePath(Path path) {
        Path result = getBaseDir().resolve(path);

        if (!result.startsWith(getBaseDir())) {
            throw new SmithyBuildException(String.format(
                    "Paths must be relative to the base directory, %s, but found %s",
                    getBaseDir(),
                    result));
        }

        return result;
    }

    /**
     * Adds a file to the result using the contents of a {@link Reader}.
     *
     * <p>This method will write the contents of a Reader to a file.
     *
     * @param path Relative path to the file to create.
     * @param fileContentsReader Reader to consume and write to the file.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    Path writeFile(Path path, Reader fileContentsReader);

    /**
     * Adds a file to the result using the contents of an {@link InputStream}.
     *
     * <p>This method will write the contents of an input stream to a file.
     *
     * @param path Relative path to the file to create.
     * @param fileContentsInputStream InputStream to consume and write to the file.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    Path writeFile(Path path, InputStream fileContentsInputStream);

    /**
     * Adds a file to the result using the contents of a resource loaded by
     * calling {@link Class#getResourceAsStream(String)}.
     *
     * <p>This method should be preferred when writing class resources to the manifest
     * since it handles closing the created {@code InputStream} and avoids
     * tripping up tools like SpotBugs.
     *
     * @param path Relative path to the file to create.
     * @param klass Class to load the resource from.
     * @param resource Path to the resource to load.
     * @return Returns the resolved path.
     */
    default Path writeFile(Path path, Class klass, String resource) {
        try (InputStream inputStream = klass.getResourceAsStream(resource)) {
            return writeFile(path, inputStream);
        } catch (IOException e) {
            throw new SmithyBuildException("Error loading class resource from " + resource + ": " + e.getMessage(), e);
        }
    }

    /**
     * Adds a file to the result using the contents of a resource loaded by
     * calling {@link Class#getResourceAsStream(String)}.
     *
     * @param path Relative path to the file to create.
     * @param klass Class to load the resource from.
     * @param resource Path to the resource to load.
     * @return Returns the resolved path.
     */
    default Path writeFile(String path, Class klass, String resource) {
        return writeFile(Paths.get(path), klass, resource);
    }

    /**
     * Adds a UTF-8 encoded file to the result.
     *
     * <p>This method will write the contents of a string to a file.
     *
     * @param path Relative path to the file to create.
     * @param fileContentsText String value to write to the file.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    default Path writeFile(Path path, String fileContentsText) {
        return writeFile(path, new StringReader(fileContentsText));
    }

    /**
     * Adds a UTF-8 encoded file to the result.
     *
     * <p>This method will write the contents of a string to a file.
     *
     * @param path Relative path to the file to create.
     * @param fileContentsText String value to write to the file.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    default Path writeFile(String path, String fileContentsText) {
        return writeFile(Paths.get(path), fileContentsText);
    }

    /**
     * Adds a file to the result using the contents of a {@link Reader}.
     *
     * <p>This method will write the contents of a Reader to a file.
     *
     * @param path Relative path to the file to create.
     * @param fileContentsReader Reader to consume and write to the file.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    default Path writeFile(String path, Reader fileContentsReader) {
        return writeFile(Paths.get(path), fileContentsReader);
    }

    /**
     * Adds a file to the result using the contents of an {@link InputStream}.
     *
     * <p>This method will write the contents of an input stream to a file.
     *
     * @param path Relative path to the file to create.
     * @param fileContentsInputStream InputStream to consume and write to the file.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    default Path writeFile(String path, InputStream fileContentsInputStream) {
        return writeFile(Paths.get(path), fileContentsInputStream);
    }

    /**
     * Adds a Node artifact, converting it automatically to JSON.
     *
     * @param path Relative path to write to.
     * @param node Node data to write to JSON.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    default Path writeJson(Path path, Node node) {
        return writeFile(path, Node.prettyPrintJson(node).trim() + "\n");
    }

    /**
     * Adds a Node artifact, converting it automatically to JSON.
     *
     * @param path Relative path to write to.
     * @param node Node data to write to JSON.
     * @return Returns the resolved path.
     */
    @SuppressWarnings("unused")
    default Path writeJson(String path, Node node) {
        return writeJson(Paths.get(path), node);
    }

    /**
     * Checks if the given file is stored in the manifest.
     *
     * @param file File to check.
     * @return Return true if the file exists in the manifest.
     */
    default boolean hasFile(Path file) {
        return getFiles().contains(resolvePath(file));
    }

    /**
     * Checks if the given file is stored in the manifest.
     *
     * @param file File to check.
     * @return Return true if the file exists in the manifest.
     */
    default boolean hasFile(String file) {
        return hasFile(Paths.get(file));
    }

    /**
     * Gets the paths to files stored under a prefix.
     *
     * @param path Path prefix.
     * @return Returns the matching file paths in sorted order.
     */
    default List<Path> getFilesIn(Path path) {
        Path resolved = resolvePath(path);
        return getFiles().stream().filter(file -> file.startsWith(resolved)).collect(Collectors.toList());
    }

    /**
     * Gets the paths to files stored under a prefix.
     *
     * @param path Path prefix.
     * @return Returns the matching file paths.
     */
    default List<Path> getFilesIn(String path) {
        return getFilesIn(Paths.get(path));
    }
}
