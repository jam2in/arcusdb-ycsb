package com.jam2in.arcusdb.client;

import com.jam2in.arcusdb.proto.ArcusDbProto.CompareExpression;
import com.jam2in.arcusdb.proto.ArcusDbProto.Expression;
import com.jam2in.arcusdb.proto.ArcusDbProto.FilterExpression;
import java.util.Objects;

/**
 * Factory methods for ArcusDB filter expressions.
 */
public final class Filters {

  private Filters() {
  }

  public static FilterExpression eq(String field, String value) {
    return compare(
        CompareExpression.Operation.EQ,
        field,
        Expression.newBuilder().setStrVal(Objects.requireNonNull(value, "value")).build()
    );
  }

  public static FilterExpression eq(String field, long value) {
    return compare(
        CompareExpression.Operation.EQ,
        field,
        Expression.newBuilder().setIntVal(value).build()
    );
  }

  private static FilterExpression compare(
      CompareExpression.Operation operation, String field, Expression rhs) {
    String fieldName = Objects.requireNonNull(field, "field").trim();
    if (fieldName.isEmpty()) {
      throw new IllegalArgumentException("field must not be blank");
    }

    return FilterExpression.newBuilder()
        .setExpr(CompareExpression.newBuilder()
            .setOp(operation)
            .setLhs(Expression.newBuilder().setFieldRef(fieldName))
            .setRhs(rhs)).build();
  }
}
