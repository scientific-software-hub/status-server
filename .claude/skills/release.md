# Release Skill

Guide the user through releasing a new version of StatusServer using the Maven Release Plugin.

## Instructions

When this skill is invoked, follow these steps in order. Explain each step and wait for confirmation before proceeding to the next one.

### Pre-flight checks

1. Verify the working tree is clean:
   ```bash
   git status
   ```
   If there are uncommitted changes, tell the user to commit or stash them first.

2. Check the current SNAPSHOT version:
   ```bash
   grep -m1 '<version>' pom.xml
   ```

3. Verify the remote is reachable:
   ```bash
   git fetch origin
   ```

### Clone to a clean release directory

4. Clone the current repo locally to avoid polluting the working tree:
   ```bash
   git clone . release
   cd release
   ```

5. Verify the build passes (skip tests if no Tango environment):
   ```bash
   mvn clean package -Dmaven.test.skip=true
   ```

### Release preparation

6. Run `release:prepare` inside the `release/` clone. This will:
   - Ask for the release version (e.g. `4.0.6`) and the next development version (e.g. `4.0.7-SNAPSHOT`)
   - Strip `-SNAPSHOT`, update `pom.xml`, commit, and tag
   - Bump to the next SNAPSHOT and commit again

   ```bash
   mvn release:prepare
   ```

   If you want non-interactive mode (accept Maven defaults):
   ```bash
   mvn release:prepare -B
   ```

7. Verify the two new commits and the tag:
   ```bash
   git log --oneline -4
   git tag | tail -5
   ```

### Release perform (build + publish)

8. Build and publish the release artifact and Docker image:
   ```bash
   mvn release:perform
   ```
   This checks out the tagged commit into `target/checkout/` and runs `mvn deploy` there, which also triggers the Docker image build and push.

9. Push the release commits and tag back to the origin (the real repo):
   ```bash
   git push origin master --tags
   ```

### Cleanup

10. Remove the temporary release directory:
    ```bash
    cd ..
    rm -rf release
    ```

11. Pull the updated commits into the original working directory:
    ```bash
    git pull
    ```

### Verify

12. Confirm the tag is visible on the remote:
    ```bash
    git ls-remote --tags origin | tail -5
    ```

### Rollback (if something went wrong before `release:perform`)

If `release:prepare` succeeded but you need to abort, from inside `release/`:
```bash
mvn release:rollback
```
This removes the tag and reverts the two release commits. Then fix the problem and start over. Finally delete the `release/` directory.

---

**Notes:**
- The Maven Release Plugin expects `<scm>` to be configured in `pom.xml` with the correct repo URL.
- If Docker push fails, you can re-run just the deploy phase from the checkout directory: `cd target/checkout && mvn deploy`.
- After a successful release, `pom.xml` on `master` will already be at the next SNAPSHOT version.
