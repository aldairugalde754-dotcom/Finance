package com.goccorp.finance.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goccorp.finance.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginTest {

    // Regla que lanza la actividad antes de cada prueba
    @get:Rule
    val activityRule = ActivityScenarioRule(Login::class.java)

    @Test
    fun checkLoginViewsAreVisible() {
        // Verifica que los elementos principales existan en pantalla
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
        onView(withId(R.id.etPassword)).check(matches(isDisplayed()))
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyLoginFields() {
        // Haz clic en el botón de Login sin escribir nada
        onView(withId(R.id.btnLogin)).perform(click())
        
        // Verifica que seguimos en la pantalla de login (el campo email sigue visible)
        onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
    }

    @Test
    fun testWritingEmailAndPassword() {
        // Escribe un correo
        onView(withId(R.id.etEmail))
            .perform(typeText("test@example.com"), closeSoftKeyboard())

        // Verifica que el texto se escribió correctamente
        onView(withId(R.id.etEmail))
            .check(matches(withText("test@example.com")))
            
        // Escribe una contraseña
        onView(withId(R.id.etPassword))
            .perform(typeText("123456"), closeSoftKeyboard())
    }
}
