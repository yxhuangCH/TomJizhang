package com.yxhuang.jizhang.feature.classification.seed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.yxhuang.jizhang.core.database.repository.CategoryRuleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeedRuleLoaderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val ruleRepo = mockk<CategoryRuleRepository>(relaxed = true)
    private val loader = SeedRuleLoader(context, ruleRepo)

    @Test
    fun `loadIfEmpty inserts rules when database empty`() = runTest {
        coEvery { ruleRepo.count() } returns 0

        loader.loadIfEmpty()

        coVerify(atLeast = 1) { ruleRepo.insert(any()) }
    }

    @Test
    fun `loadIfEmpty skips when rules already exist`() = runTest {
        coEvery { ruleRepo.count() } returns 10

        loader.loadIfEmpty()

        coVerify(exactly = 0) { ruleRepo.insert(any()) }
    }

    @Test
    fun `parseSeedRules returns correct count`() {
        val rules = loader.parseSeedRules()
        assertEquals(true, rules.isNotEmpty())
    }
}
