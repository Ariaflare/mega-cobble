{ name: 'Piercing Drill', onModifyMove(move) { if (move.flags['contact']) { move.flags['protect'] = 0; } } }
