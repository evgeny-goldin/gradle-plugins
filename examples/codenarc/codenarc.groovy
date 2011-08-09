
ruleset {

    description 'Test CodeNarc RuleSet'

    ruleset( "http://codenarc.sourceforge.net/StarterRuleSet-AllRulesByCategory.groovy.txt" ) {

        DuplicateNumberLiteral ( enabled : false )
        DuplicateStringLiteral ( enabled : false )
        BracesForClass         ( enabled : false )

    }
}

