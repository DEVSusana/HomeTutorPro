## 2026-05-14 - Optimize LazyColumn Recomposition
**Learning:** Found unnecessary object creation inside a LazyColumn `items` loop and missing key configuration in Jetpack Compose, which lead to high memory churn and sub-optimal recomposition behavior when rendering resource lists.
**Action:** Lift static object instantiation (`SimpleDateFormat`) out of the loop and implement `remember` to ensure it's evaluated only once per lifecycle. Also explicitly provide a unique key for each item in the `LazyColumn`.
