package xyz.uthofficial.arnyan.env.result

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import xyz.uthofficial.arnyan.env.error.ArnyanError
import xyz.uthofficial.arnyan.env.error.ConfigurationError

class BindingTest : FunSpec({

    test("bind should catch generic exceptions and wrap them in BindException") {
        val result = binding<Int, ArnyanError> {
            binding({ ConfigurationError.InvalidConfiguration("Wrapped: ${it.message}") }) {
                throw RuntimeException("Original error")
            }
        }
        
        result.shouldBeInstanceOf<Result.Failure<ConfigurationError.InvalidConfiguration>>()
        ((result as Result.Failure).error as ConfigurationError.InvalidConfiguration).message shouldBe "Wrapped: Original error"
    }

    test("bind should propagate success value") {
        val result = binding<Int, ArnyanError> {
            binding({ ConfigurationError.InvalidConfiguration("Should not happen") }) {
                42
            }
        }

        result.shouldBeInstanceOf<Result.Success<Int>>()
        result.value shouldBe 42
    }
    
    test("bind should not interfere with existing Result.bind failures") {
        val innerError = ConfigurationError.InvalidConfiguration("Inner error")
        
        val result = binding<Int, ArnyanError> {
            binding({ ConfigurationError.InvalidConfiguration("Wrapped: ${it.message}") }) {
                Result.Failure(innerError).bind() 
            }
        }

        result.shouldBeInstanceOf<Result.Failure<ConfigurationError.InvalidConfiguration>>()
        (result as Result.Failure).error shouldBe innerError
    }
})
