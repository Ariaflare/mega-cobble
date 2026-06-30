{ name: 'Fire Mane', onBasePowerPriority: 19, onBasePower(basePower, attacker, defender, move) { if (move.type === 'Fire') { return this.chainModify(1.5); } } }
