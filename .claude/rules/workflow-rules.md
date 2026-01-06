# Branching Strategy

- `main`: The stable branch deployed to the production environment.
- `develop`: Features are integrated and tested.
- `feature`: Branches created for task development.

---

1. All new features or issues must be developed in a `feature/feature-name` branch branched off from `develop`.
2. Once development is complete, a Pull Request(PR) is made to the `develop` branch. Continuous integration and testing are performed directly within the `develop` branch.
3. Code that has been fully verified in the `develop` branch is merged into the `main` branch. Merges to `main` are continuously deployed to the production environment.

---

- Do not operate a separate `hotfix` branch. Emergency fixes are handled through the standard feature development flow to focus on rapid development.