ifeq ($(OS),Windows_NT)
build-RecipesFunction:
	cmd /c if not exist "$(ARTIFACTS_DIR)\lib" mkdir "$(ARTIFACTS_DIR)\lib"
	cmd /c copy /Y target\recipes-api-rest-1.0.0.jar "$(ARTIFACTS_DIR)\lib"
else
build-RecipesFunction:
	mkdir -p $(ARTIFACTS_DIR)/lib
	cp target/recipes-api-rest-1.0.0.jar $(ARTIFACTS_DIR)/lib/
endif
