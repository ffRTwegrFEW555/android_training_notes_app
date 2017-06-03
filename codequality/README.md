# android-codequality-sample
Gradle sample for android project with lint, checkstyle, findbugs and pmd.

# Usage
1. Put codequality directory into directory of your root project
2. Add to build.gradle of any java-project next line:  
```
apply from: "${rootProject.projectDir}/codequality/codequality.gradle"
```
If you want to use it for android-project, add next two lines:  
```
apply from: "${rootProject.projectDir}/codequality/codequality.gradle"
apply from: "${rootProject.projectDir}/codequality/codequality-android.gradle"
```
Also you can add to root build.gradle for applying for all modules:
```
subprojects {
    apply plugin: 'com.android.application'
    apply from: "${rootProject.projectDir}/codequality/codequality.gradle"
    apply from: "${rootProject.projectDir}/codequality/codequality-android.gradle"
}
```
3. Runnig. You can run single check:
```
./gradlew checkstyleMain
./gradlew pmdMain
./gradlew lintRelease
./gradlew findbugsTest
./gradlew lint
./gradlew pmd
```
Or you can run all checks (exclude test)
```
./gradlew check -x test
```
4. All reports will be placed in
```
${rootProject.buildDir}/reports/${analyzerName}/${project.name}/$htmlReportFileName
```  
For example:
```
build/reports/lint/my-application/lint.html
build/reports/findbugs/my-application/main.html
```
