package com.test

import org.gcontracts.annotations.Invariant

@Invariant({ j == 5 })
class Something
{
    final j = 5
}
