package de.faceco.mashovapi.components;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MashovJustification implements Comparable<MashovJustification> {
  private String justification;
  private boolean hidden;
  private int justificationId;
  private int displayOrder;
  
  public String getJustification() {
    return justification;
  }
  
  public boolean isHidden() {
    return hidden;
  }
  
  public int getJustificationId() {
    return justificationId;
  }
  
  public int getDisplayOrder() {
    return displayOrder;
  }
  
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", justification)
        .add("hidden", hidden)
        .add("justificationId", justificationId)
        .add("displayOrder", displayOrder)
        .toString();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MashovJustification that = (MashovJustification) o;
    return hidden == that.hidden &&
        justificationId == that.justificationId &&
        displayOrder == that.displayOrder &&
        Objects.equals(justification, that.justification);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(justification, hidden, justificationId, displayOrder);
  }
  
  @Override
  public int compareTo(@NotNull MashovJustification j) {
    return ComparisonChain.start()
        .compare(displayOrder, j.displayOrder)
        .compare(justification, j.justification)
        .compare(justificationId, j.justificationId)
        .result();
  }
}
