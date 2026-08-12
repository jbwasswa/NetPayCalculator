# Uganda Net Pay Calculator

Android salary calculator for Ugandan PAYE, including gross-to-net and net-to-gross modes.

## Current MVP

- Native Android app scaffolded with Kotlin and Jetpack Compose.
- PAYE rules are versioned by tax year.
- Supports resident and non-resident calculations.
- Separates taxable allowances from non-taxable reimbursements.
- Supports employee NSSF deduction and employer NSSF display.
- Supports reverse calculation from desired net pay to required gross pay.
- Includes focused unit tests for the calculator engine.

## PAYE Rules Included

- FY 2025/26 resident structure using the old UGX 235,000 nil threshold.
- FY 2026/27 resident structure using the new UGX 335,000 nil threshold.
- Non-resident structures are modeled separately and should be reviewed against the final official payroll guidance before release.

## Allowance Treatment

The calculator treats taxable allowances as employment income:

```text
PAYE income = basic/gross salary + taxable allowances
cash earnings = basic/gross salary + taxable allowances + non-taxable reimbursements
NSSF contribution base = basic/gross salary + taxable allowances
net pay = cash earnings - PAYE - employee NSSF - other deductions
```

Non-taxable reimbursements are included in cash paid to the employee but excluded from PAYE income.

## Build Notes

This project expects Android Studio or a local Gradle/Android SDK setup with Java 17.

From the project root:

```powershell
.\gradlew test
.\gradlew assembleDebug
```

The current machine did not expose a global Gradle command or Android SDK path during scaffolding, so build verification still needs Android tooling.

## Download APK From GitHub

Every push to `main` runs the **Build Debug APK** workflow.

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Open the latest **Build Debug APK** run.
4. Download the artifact named **NetPayCalculator-debug-apk**.
5. Unzip it and install `app-debug.apk` on your Android phone.

If release signing secrets are configured, the workflow also uploads **NetPayCalculator-release-apk**. That is the preferred APK for repeated installation as updates.

## APK Updates

Android installs a new APK over the old one only when:

- `applicationId` stays the same: `com.jbwasswa.ugandanetpay`
- the APK is signed with the same certificate as the installed APK
- `versionCode` is higher than the installed APK

This project keeps the package name stable and has release signing support through GitHub repository secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

If you already installed an earlier debug APK signed with a temporary debug key, you may need to uninstall once before installing the first properly signed release APK. After that, future signed release APKs should install as updates.
