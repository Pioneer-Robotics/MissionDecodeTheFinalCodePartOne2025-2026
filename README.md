# Pioneer Robotics: Decode 2025-2026

## Referencing the Original README

For detailed information, refer to the [FTC Robot Controller repository README](https://github.com/FIRST-Tech-Challenge/FtcRobotController/blob/master/README.md).

## Official FTC Java Documentation

For official Java documentation, visit the [FTC JavaDocs](https://javadoc.io/doc/org.firstinspires.ftc).

## Keeping the Repository Updated

To stay aligned with the upstream FTC repository:

1. Add the upstream repository:
  ```bash
  git remote add upstream https://github.com/FIRST-Tech-Challenge/FtcRobotController
  ```

2. Fetch the latest updates:
  ```bash
  git fetch upstream
  ```

3. Merge the updates:
  ```bash
  git merge upstream/master
  ```

4. Address any merge conflicts as necessary.

Regular updates ensure you benefit from the latest features and fixes.

## Formatting Kotlin Code

Use `ktlint` to format your Kotlin files:

```bash
ktlint -F <filename>
```