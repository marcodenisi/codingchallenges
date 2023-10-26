# Write Your Own wc Tool

Simple Java 17 script implementing a `wc` command clone. 

[Picocli](https://picocli.info/) is used to read command line options and parameters.

[JBang](https://www.jbang.dev/) is used to make this script:
* self-contained (even if a little bit messy)
* simply runnable from the command line without packaging it

### How to run

```bash
jbang ccwc path/to/file
```
or, using standard input
```bash
echo "test" | jbang ccwc
```

There are 4 different flags available (`-cmwl`), please refer to the official `wc` documentation for more information.
