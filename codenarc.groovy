
ruleset {

    description 'Gradle plugins CodeNarc RuleSet'

    ruleset( 'http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt' ) {

        DuplicateNumberLiteral   ( enabled : false )
        DuplicateStringLiteral   ( enabled : false )
        BracesForClass           ( enabled : false )
        BracesForMethod          ( enabled : false )
        BracesForIfElse          ( enabled : false )
        BracesForForLoop         ( enabled : false )
        BracesForTryCatchFinally ( enabled : false )
        JavaIoPackageAccess      ( enabled : false )
        ThrowRuntimeException    ( enabled : false )
        FactoryMethodName        ( enabled : false )
        MethodName               ( enabled : false )
        SpaceAfterOpeningBrace   ( enabled : false )
        SpaceBeforeOpeningBrace  ( enabled : false )
        SpaceBeforeClosingBrace  ( enabled : false )

        VariableName             ( finalRegex          : /\w+/ )
        PropertyName             ( staticFinalRegex    : /\w+/ )
        AbcComplexity            ( maxMethodComplexity : 70  )
        LineLength               ( length              : 180 )
    }
}

