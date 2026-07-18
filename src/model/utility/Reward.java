package model.utility;

/** A source-backed quest reward. Variable formulas are limited to formulas in the workbook. */
public class Reward {
    public enum AmountRule {
        FIXED,
        VARIABLE_DIVIDED_BY_100,
        TWENTY_MINUS_VARIABLE,
        VARIABLE
    }

    private RewardType type;
    private int amount;
    private String target;
    private AmountRule amountRule;
    private String canonicalText;

    public Reward() {
    }

    public Reward(RewardType type, int amount, String target,
                  AmountRule amountRule, String canonicalText) {
        this.type = type;
        this.amount = amount;
        this.target = target;
        this.amountRule = amountRule == null ? AmountRule.FIXED : amountRule;
        this.canonicalText = canonicalText;
    }

    public RewardType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public String getTarget() {
        return target;
    }

    public AmountRule getAmountRule() {
        return amountRule;
    }

    public String getCanonicalText() {
        return canonicalText;
    }

    public int resolveAmount(String variableValue) {
        if (amountRule == AmountRule.FIXED) {
            return amount;
        }
        if (variableValue == null || variableValue.isBlank()) {
            throw new IllegalStateException("This reward requires a canonical variable value.");
        }
        int variable;
        try {
            variable = Integer.parseInt(variableValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Reward variable must be numeric: " + variableValue);
        }
        return switch (amountRule) {
            case VARIABLE_DIVIDED_BY_100 -> variable / 100;
            case TWENTY_MINUS_VARIABLE -> 20 - variable;
            case VARIABLE -> variable;
            default -> amount;
        };
    }
}
