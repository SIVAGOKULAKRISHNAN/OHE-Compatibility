# Build fix 2

The second build log exposed 12 remaining compile errors. Fixed:

- Create 6.0.10-280 compile dependencies now explicitly include Ponder 1.0.82, Flywheel 1.0.6 API/runtime, and Registrate MC1.21-1.3.0+67.
- HorizontalDirectionalBlock codec methods now return the required concrete generic type.
- Section block entity constructor now accepts BlockEntityType and is supplied via the Builder lambda.
- CEE TransformerBlockEntity instances now use the required `(type, pos, state)` constructor lambda.
